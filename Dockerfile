FROM python:3.11-slim

WORKDIR /app

# 프로젝트 디렉터리 안의 모든 파일을 복사. (service_account.json 포함)
COPY . /app

RUN pip install --upgrade pip
# 필요 패키지 설치
RUN pip install --no-cache-dir -r requirements.txt

# Gunicorn + UvicornWorker로 ASGI 실행
CMD ["gunicorn", "-k", "uvicorn.workers.UvicornWorker", "main:app", "--bind", "0.0.0.0:8080","--http","http"]
