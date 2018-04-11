# Manta Monitor

## Docker
Running via Docker:

```
docker run -d \
    --name manta-monitor-1
    --memory 1G \
    --label triton.cns.services=manta-monitor \
    -e ENV=production \
    -e HONEYBADGER_API_KEY=XXXXXXXX \
    -e CONFIG_FILE=manta:///user/stor/manta-monitor-config.json \
    -e MANTA_USER=user \
    -e "MANTA_PUBLIC_KEY=$(cat $HOME/.ssh/id_rsa.pub)" \
    -e "MANTA_PRIVATE_KEY=$(cat $HOME/.ssh/id_rsa | base64 -w0)" \
    -e "MANTA_URL=https://us-east.manta.joyent.com" \
    -e MANTA_TIMEOUT=4000 \
    dekobon/manta-monitor
```
