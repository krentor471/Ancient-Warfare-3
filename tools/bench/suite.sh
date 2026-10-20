# Full benchmark. The first scenario warms up the JIT and is discarded.
cd "$(dirname "$0")"
timeout 300 python3 bench.py army 100 > /dev/null
timeout 900 python3 bench.py idle 400
timeout 900 python3 bench.py soldiers 200
timeout 900 python3 bench.py battle 100 200
timeout 900 python3 bench.py army 100 200
