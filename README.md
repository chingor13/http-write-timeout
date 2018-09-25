# Java HTTP Write Timeout Test

This repo demonstrates how to make an HTTP upload request hang indefinitely even with read and
connect timeouts configured.

## Setup

1. Create an file to upload:

```bash
$> dd if=/dev/zero of=src/main/resources/file.txt count=65536 bs=1024
```

2. Run toxiproxy:

```bash
$> docker run --net=host shopify/toxiproxy
```

3. Run the upload server. Using PHP because we don't care about the response, we just need something
to accept a file upload.

```bash
$> php -S 127.0.0.1:8080 -t server
```

