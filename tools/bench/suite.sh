cd "$(dirname "$0")"
timeout 900 python3 bench.py idle 100 400
timeout 900 python3 bench.py soldiers 100 200
timeout 900 python3 bench.py battle 100 200
