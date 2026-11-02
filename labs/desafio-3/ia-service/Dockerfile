# Imagem propria do servico de IA: e o que permite publicar e escalar sem tocar no Core.
FROM python:3.14-slim

WORKDIR /srv

# Dependencias em camada separada, reaproveitada enquanto requirements.txt nao mudar.
COPY requirements.txt .
RUN pip install --no-cache-dir -r requirements.txt

COPY app ./app

RUN useradd --create-home ia
USER ia

EXPOSE 8000

HEALTHCHECK --interval=10s --timeout=2s --retries=3 \
  CMD python -c "import urllib.request; urllib.request.urlopen('http://localhost:8000/health', timeout=1)"

CMD ["uvicorn", "app.main:app", "--host", "0.0.0.0", "--port", "8000"]
