FROM python:3.12-slim
WORKDIR /bootstrap
COPY docker/requirements.txt ./
RUN pip install --no-cache-dir -r requirements.txt
COPY docker/bootstrap.py ./
COPY zaomeng-backend/database/mysql/ /bootstrap/sql/
COPY docker/storage-r2.example.json /bootstrap/storage-r2.example.json
ENTRYPOINT ["python", "/bootstrap/bootstrap.py"]
