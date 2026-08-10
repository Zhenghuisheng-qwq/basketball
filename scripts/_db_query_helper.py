import pymysql, json, sys, io
sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')
conn = pymysql.connect(host='192.168.31.48', port=3306, user='root', password='123456', database='basketball', charset='utf8mb4', cursorclass=pymysql.cursors.DictCursor)
try:
    with conn.cursor() as cur:
        cur.execute(sys.argv[1])
        print(json.dumps(list(cur.fetchall()), default=str, ensure_ascii=False))
finally:
    conn.close()
