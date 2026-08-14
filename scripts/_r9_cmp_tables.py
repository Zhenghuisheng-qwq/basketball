#!/usr/bin/env python3
# -*- coding: utf-8 -*-
import pymysql
c = pymysql.connect(host='192.168.31.48', user='root', password='123456', charset='utf8mb4')
cur = c.cursor()
cur.execute('SELECT table_name FROM information_schema.tables WHERE table_schema=%s AND table_name LIKE %s ORDER BY 1', ('basketball_r9_fresh', 'basket_%'))
fresh = {r[0] for r in cur.fetchall()}
cur.execute('SELECT table_name FROM information_schema.tables WHERE table_schema=%s AND table_name LIKE %s ORDER BY 1', ('basketball', 'basket_%'))
dev = {r[0] for r in cur.fetchall()}
print('DEV only', len(dev - fresh))
for t in sorted(dev - fresh):
    print('  D', t)
print('FRESH only', len(fresh - dev))
for t in sorted(fresh - dev):
    print('  F', t)
print('common', len(dev & fresh))
c.close()
