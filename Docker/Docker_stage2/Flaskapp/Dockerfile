FROM alpine

RUN apk add --no-cache python3 py3-pip 

WORKDIR /app

COPY app.py .

COPY requirements.txt .

RUN pip install --break-system-packages --no-cache-dir -r requirements.txt

EXPOSE 4000

CMD ["python3", "app.py"]
