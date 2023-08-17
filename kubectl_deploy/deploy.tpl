apiVersion: apps/v1
kind: Deployment
metadata:
  name: caab-ui
  namespace: laa-ccms-civil
  labels:
    app.kubernetes.io/name: caab-ui
spec:
  replicas: 1
  selector:
    matchLabels:
      app.kubernetes.io/name: caab-ui
  template:
    metadata:
      labels:
        app.kubernetes.io/name: caab-ui
    spec:
      containers:
        - name: caab-ui
          image: ${ECR_URL}:${IMAGE_TAG}
          ports:
            - containerPort: 8080
          env:
          - name: SAML_METADATA_URI
            valueFrom:
              secretKeyRef:
                name: saml-metadata-uri
                key: saml-metadata-uri
          - name: SAML_PRIVATE_KEY
            valueFrom:
              secretKeyRef:
                name: saml-metadata-uri
                key: saml-private-key
          - name: SAML_CERTIFICATE
            valueFrom:
              secretKeyRef:
                name: saml-metadata-uri
                key: saml-certificate
          - name: CAAB_DATA_API_HOSTNAME
            valueFrom:
              secretKeyRef:
                name: saml-metadata-uri
                key: caab-data-api-hostname
          - name: CAAB_SOA_API_HOSTNAME
            valueFrom:
              secretKeyRef:
                name: saml-metadata-uri
                key: caab-soa-gateway-api-hostname
          - name: CAAB_API_HOSTNAME
            valueFrom:
              secretKeyRef:
                name: saml-metadata-uri
                key: caab-api-hostname

