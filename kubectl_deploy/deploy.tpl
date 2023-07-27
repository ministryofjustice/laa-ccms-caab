apiVersion: apps/v1
kind: Deployment
metadata:
  name: laa-ccms-civil
spec:
  replicas: 1
  selector:
    matchLabels:
      app: laa-ccms-civil
  template:
    metadata:
      labels:
        app: laa-ccms-civil
    spec:
      containers:
        - name: laa-ccms-civil
          image: ${ECR_URL}:${IMAGE_TAG}
          ports:
            - containerPort: 5000

