import json

import simulador


class DispositivoFalso:
    device_id = "sensor-01"

    def __init__(self):
        self.mensagens = []
        self.eventos_conexao = []

    def publicar(self, evento):
        self.mensagens.append(json.dumps(evento))

    def publicar_bruto(self, corpo):
        self.mensagens.append(corpo)

    def aguardar_confirmacoes(self):
        self.eventos_conexao.append(("confirmados", len(self.mensagens)))

    def perder_conexao(self):
        self.eventos_conexao.append(("perdeu", len(self.mensagens)))

    def reconectar(self):
        self.eventos_conexao.append(("reconectou", len(self.mensagens)))


def test_evento_tem_todos_os_campos_do_contrato():
    evento = simulador.novo_evento("sensor-01", "presenca", 7, "ci-cd")

    assert set(evento) == {"eventId", "deviceId", "tipo", "alunoId", "atividadeId", "timestamp"}
    assert evento["timestamp"].endswith("Z")


def test_cada_evento_novo_tem_event_id_proprio():
    primeiro = simulador.novo_evento("sensor-01", "presenca", 7, "ci-cd")
    segundo = simulador.novo_evento("sensor-01", "presenca", 7, "ci-cd")

    assert primeiro["eventId"] != segundo["eventId"]


def test_topico_identifica_o_dispositivo_na_posicao_que_o_core_confere():
    assert simulador.topico_eventos("lab-a", "sensor-01").split("/")[3] == "sensor-01"


def test_modo_normal_publica_os_tres_tipos():
    dispositivo = DispositivoFalso()

    simulador.modo_normal(dispositivo, 7, "ci-cd")

    assert [json.loads(m)["tipo"] for m in dispositivo.mensagens] == ["presenca", "inicio", "conclusao"]


def test_modo_reenvio_repete_o_mesmo_event_id():
    dispositivo = DispositivoFalso()

    simulador.modo_reenvio(dispositivo, 7, "ci-cd")

    primeiro, segundo = [json.loads(m) for m in dispositivo.mensagens]
    assert primeiro["eventId"] == segundo["eventId"]


def test_modo_offline_gera_eventos_entre_a_perda_e_a_reconexao():
    dispositivo = DispositivoFalso()

    simulador.modo_offline(dispositivo, 7, "ci-cd", segundos_offline=0)

    assert ("perdeu", 1) in dispositivo.eventos_conexao
    assert ("reconectou", 3) in dispositivo.eventos_conexao


def test_modo_invalido_nao_publica_evento_valido_do_proprio_dispositivo():
    dispositivo = DispositivoFalso()

    mensagens = simulador.modo_invalido(dispositivo, 7, "ci-cd")

    assert len(mensagens) == 3
    assert "alunoId" not in json.loads(mensagens[1])
    assert json.loads(mensagens[2])["deviceId"] != "sensor-01"


def test_mqtt_tem_menos_bytes_de_protocolo_que_http():
    corpo = json.dumps(simulador.novo_evento("sensor-01", "conclusao", 7, "ci-cd")).encode()

    mqtt_bytes = simulador.tamanho_publish_mqtt(simulador.topico_eventos("lab-a", "sensor-01"), corpo)

    assert len(corpo) < mqtt_bytes < simulador.tamanho_post_http(corpo)


def test_tamanho_mqtt_conta_o_byte_extra_do_tamanho_restante():
    assert simulador.tamanho_publish_mqtt("t", b"x" * 10) == 1 + 1 + 2 + 1 + 2 + 10
    assert simulador.tamanho_publish_mqtt("t", b"x" * 200) == 1 + 2 + 2 + 1 + 2 + 200
