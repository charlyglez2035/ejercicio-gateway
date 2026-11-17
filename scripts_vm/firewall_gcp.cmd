@echo off
REM Ejecutar en el Google Cloud SDK Shell de Windows (NO dentro de la VM).
REM Abre los puertos del monolito (3000) y de los microservicios (5000-5005) hacia maquina02.
REM Si ya existe la regla, gcloud mostrara un error "already exists": se puede ignorar.

gcloud compute firewall-rules create permitir-locust-libreria ^
  --project ejercicio02-505521 ^
  --direction INGRESS ^
  --action ALLOW ^
  --rules tcp:3000,tcp:5000-5005 ^
  --source-ranges 0.0.0.0/0 ^
  --target-tags locust-libreria

gcloud compute instances add-tags maquina02 ^
  --zone northamerica-south1-c ^
  --tags locust-libreria
