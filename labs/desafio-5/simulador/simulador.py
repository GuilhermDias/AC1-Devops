"""Simulador de dispositivo de laboratorio: publica eventos JSON por MQTT, com QoS 1.

Modos:
  normal    publica um evento presenca, um inicio e um conclusao
  reenvio   publica o mesmo evento de conclusao duas vezes (mesmo eventId)
  offline   publica presenca, perde a conexao, gera eventos e os entrega ao reconectar
  invalido  publica mensagens que o Core deve descartar
  tamanho   mostra o tamanho de um evento em MQTT e em HTTP, sem conectar
"""
import argparse
import json
import threading
import time
import uuid
from datetime import datetime, timezone

QOS_AT_LEAST_ONCE = 1


def novo_evento(device_id, tipo, aluno_id, atividade_id, event_id=None):
    return {
        "eventId": event_id or str(uuid.uuid4()),
        "deviceId": device_id,
        "tipo": tipo,
        "alunoId": aluno_id,
        "atividadeId": atividade_id,
        "timestamp": datetime.now(timezone.utc).strftime("%Y-%m-%dT%H:%M:%SZ"),
    }


def topico_eventos(lab, device_id):
    return f"labs/{lab}/devices/{device_id}/events"


def topico_status(lab, device_id):
    return f"labs/{lab}/devices/{device_id}/status"


def tamanho_publish_mqtt(topico, corpo):
    """Bytes de um PUBLISH QoS 1: tipo, tamanho restante, topico, id do pacote e corpo."""
    restante = 2 + len(topico.encode()) + 2 + len(corpo)
    bytes_do_tamanho = 1
    while restante >= 128 ** bytes_do_tamanho:
        bytes_do_tamanho += 1
    return 1 + bytes_do_tamanho + restante


def tamanho_post_http(corpo):
    """Bytes de um POST HTTP/1.1 minimo, sem contar autenticacao nem a abertura da conexao."""
    cabecalhos = (
        "POST /iot/eventos HTTP/1.1\r\n"
        "Host: plataforma.example\r\n"
        "Content-Type: application/json\r\n"
        f"Content-Length: {len(corpo)}\r\n\r\n"
    )
    return len(cabecalhos.encode()) + len(corpo)


class Dispositivo:
    """Cliente MQTT do dispositivo. A resposta de cada publicacao vem pelo callback on_publish."""

    def __init__(self, broker, porta, lab, device_id):
        import paho.mqtt.client as mqtt

        self.broker, self.porta = broker, porta
        self.lab, self.device_id = lab, device_id
        self._confirmados = 0
        self._condicao = threading.Condition()
        self._cliente = mqtt.Client(mqtt.CallbackAPIVersion.VERSION2, client_id=f"dispositivo-{device_id}",
                                    clean_session=False)
        self._cliente.on_publish = self._ao_confirmar
        # LWT: se o dispositivo sumir sem avisar, o broker publica "offline" no lugar dele.
        self._cliente.will_set(topico_status(lab, device_id), "offline", qos=QOS_AT_LEAST_ONCE, retain=True)

    def _ao_confirmar(self, client, userdata, mid, reason_code, properties):
        with self._condicao:
            self._confirmados += 1
            self._condicao.notify_all()

    def conectar(self):
        self._cliente.connect(self.broker, self.porta, keepalive=30)
        self._cliente.loop_start()
        # Retained: quem assinar depois ja recebe o ultimo estado do dispositivo.
        self._cliente.publish(topico_status(self.lab, self.device_id), "online", qos=QOS_AT_LEAST_ONCE, retain=True)
        self._publicados = 1

    def perder_conexao(self):
        self._cliente.disconnect()
        self._cliente.loop_stop()

    def reconectar(self):
        self._cliente.reconnect()
        self._cliente.loop_start()

    def publicar_bruto(self, corpo):
        self._publicados += 1
        return self._cliente.publish(topico_eventos(self.lab, self.device_id), corpo, qos=QOS_AT_LEAST_ONCE)

    def publicar(self, evento):
        return self.publicar_bruto(json.dumps(evento))

    def aguardar_confirmacoes(self, timeout=15):
        limite = time.monotonic() + timeout
        with self._condicao:
            while self._confirmados < self._publicados:
                restante = limite - time.monotonic()
                if restante <= 0:
                    raise TimeoutError(f"{self._confirmados} de {self._publicados} confirmados")
                self._condicao.wait(restante)
        return self._confirmados - 1

    def encerrar(self):
        self._cliente.publish(topico_status(self.lab, self.device_id), "offline", qos=QOS_AT_LEAST_ONCE, retain=True)
        time.sleep(0.2)
        self._cliente.disconnect()
        self._cliente.loop_stop()


def modo_normal(dispositivo, aluno, atividade):
    eventos = [novo_evento(dispositivo.device_id, tipo, aluno, atividade) for tipo in ("presenca", "inicio", "conclusao")]
    for evento in eventos:
        dispositivo.publicar(evento)
        print(f"publicado {evento['tipo']}: {evento['eventId']}")
    return eventos


def modo_reenvio(dispositivo, aluno, atividade):
    evento = novo_evento(dispositivo.device_id, "conclusao", aluno, atividade)
    for tentativa in (1, 2):
        dispositivo.publicar(evento)
        print(f"envio {tentativa} do evento {evento['eventId']}")
    return [evento, evento]


def modo_offline(dispositivo, aluno, atividade, segundos_offline):
    enviado = novo_evento(dispositivo.device_id, "presenca", aluno, atividade)
    dispositivo.publicar(enviado)
    print(f"online: publicado presenca {enviado['eventId']}")
    dispositivo.aguardar_confirmacoes()

    dispositivo.perder_conexao()
    print("conexao perdida")
    guardados = [novo_evento(dispositivo.device_id, tipo, aluno, atividade) for tipo in ("inicio", "conclusao")]
    for evento in guardados:
        dispositivo.publicar(evento)
        print(f"offline: guardado {evento['tipo']} {evento['eventId']}")
    time.sleep(segundos_offline)

    dispositivo.reconectar()
    print("reconectado")
    return [enviado, *guardados]


def modo_invalido(dispositivo, aluno, atividade):
    sem_aluno = novo_evento(dispositivo.device_id, "presenca", aluno, atividade)
    del sem_aluno["alunoId"]
    outro_dispositivo = novo_evento("outro-dispositivo", "presenca", aluno, atividade)
    mensagens = ["isto nao e json", json.dumps(sem_aluno), json.dumps(outro_dispositivo)]
    for mensagem in mensagens:
        dispositivo.publicar_bruto(mensagem)
        print(f"publicado invalido: {mensagem[:60]}")
    return mensagens


def modo_tamanho(lab, device_id, aluno, atividade):
    corpo = json.dumps(novo_evento(device_id, "conclusao", aluno, atividade)).encode()
    mqtt_bytes = tamanho_publish_mqtt(topico_eventos(lab, device_id), corpo)
    http_bytes = tamanho_post_http(corpo)
    print(f"corpo JSON: {len(corpo)} bytes")
    print(f"MQTT PUBLISH QoS 1: {mqtt_bytes} bytes ({mqtt_bytes - len(corpo)} de protocolo)")
    print(f"HTTP POST minimo: {http_bytes} bytes ({http_bytes - len(corpo)} de protocolo)")
    return mqtt_bytes, http_bytes


def main():
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--modo", choices=["normal", "reenvio", "offline", "invalido", "tamanho"], default="normal")
    parser.add_argument("--broker", default="localhost")
    parser.add_argument("--porta", type=int, default=1883)
    parser.add_argument("--lab", default="lab-a")
    parser.add_argument("--device", default="sensor-01")
    parser.add_argument("--aluno", type=int, default=1)
    parser.add_argument("--atividade", default="ci-cd")
    parser.add_argument("--segundos-offline", type=int, default=5)
    args = parser.parse_args()

    if args.modo == "tamanho":
        modo_tamanho(args.lab, args.device, args.aluno, args.atividade)
        return

    dispositivo = Dispositivo(args.broker, args.porta, args.lab, args.device)
    dispositivo.conectar()
    if args.modo == "normal":
        modo_normal(dispositivo, args.aluno, args.atividade)
    elif args.modo == "reenvio":
        modo_reenvio(dispositivo, args.aluno, args.atividade)
    elif args.modo == "offline":
        modo_offline(dispositivo, args.aluno, args.atividade, args.segundos_offline)
    else:
        modo_invalido(dispositivo, args.aluno, args.atividade)
    print(f"confirmados pelo broker (QoS 1): {dispositivo.aguardar_confirmacoes()}")
    dispositivo.encerrar()


if __name__ == "__main__":
    main()
