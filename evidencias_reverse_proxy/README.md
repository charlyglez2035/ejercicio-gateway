# Evidencias: apps de escritorio detrás del Reverse Proxy nginx

Gateway: `http://34.51.105.144` (nginx en el puerto 80 de `maquina02`). Las tres apps usan solo esta URL; ninguna llama a los puertos 3000 ni 5000-5005.

| # | Archivo | Qué demuestra |
|---|---|---|
| 01 | `01_nginx_activo_y_puertos.png` | `systemctl status nginx` y `ss -ltnp`: nginx escucha en el :80 y los servicios corren dentro de la VM |
| 02 | `02_gateway_health.png` | `http://34.51.105.144/gateway/health` responde `{"status":"ok","gateway":"nginx"}` |
| 10 | `10_electron_catalogo_via_gateway.png` | Electron con el endpoint `http://34.51.105.144/api/books/books` y las cards cargadas |
| 20 | `20_tk_gateway_semaforos.png` | Tkinter: campo **Gateway** y semáforos en verde después de **Comprobar servicios** |
| 21 | `21_tk_login_y_crud_con_jwt.png` | Tkinter: sesión iniciada y una lista protegida (Users, Pedidos o Pagos) |
| 22 | `22_tk_401_sin_jwt.png` | Tkinter con **Omitir JWT**: el gateway responde 401 |
| 30 | `30_java_health.png` | Java: diálogo "Salud vía gateway" con todo en OK |
| 31 | `31_java_libros.png` | Java: pestaña Libros (pública) y el log con el `X-Request-ID` |
| 32 | `32_java_login_autores_jwt.png` | Java: sesión iniciada y pestaña Autores o Pedidos con datos (JWT) |
| 33 | `33_java_401_sin_jwt.png` | Java con **Omitir JWT**: 401 del gateway |
| 40 | `40_nginx_access_log_3_apps.png` | Salida de `bash scripts_vm/evidencia_reverse_proxy.sh`: las peticiones de las 3 apps en el access log de nginx, identificadas por su User-Agent |
| 41 | `evidencia_reverse_proxy.txt` | La misma salida guardada como texto (registro) |

## Cómo generar el registro en la VM

```bash
cd ~/ejercicio-gateway && git pull
bash scripts_vm/evidencia_reverse_proxy.sh | tee evidencias_reverse_proxy/evidencia_reverse_proxy.txt
```
