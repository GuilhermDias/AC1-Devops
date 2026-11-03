-- 100.000 alunos para medir o ranking (ASR2). Rode so em um banco descartavel.
INSERT INTO students (name, xp_total, level)
SELECT 'Aluno ' || i,
       (i * 7919) % 100000,
       CASE
           WHEN (i * 7919) % 100000 >= 700 THEN 'DIAMANTE'
           WHEN (i * 7919) % 100000 >= 300 THEN 'OURO'
           WHEN (i * 7919) % 100000 >= 100 THEN 'PRATA'
           ELSE 'BRONZE'
       END
FROM generate_series(1, 100000) AS i;
