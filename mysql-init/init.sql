CREATE DATABASE IF NOT EXISTS auth_db;
CREATE DATABASE IF NOT EXISTS resume_db;
CREATE DATABASE IF NOT EXISTS ai_db;
CREATE DATABASE IF NOT EXISTS template_db;
CREATE DATABASE IF NOT EXISTS payment_db;
CREATE DATABASE IF NOT EXISTS export_db;

CREATE USER IF NOT EXISTS 'resumeai'@'%' IDENTIFIED BY 'resumeai';

GRANT ALL PRIVILEGES ON auth_db.* TO 'resumeai'@'%';
GRANT ALL PRIVILEGES ON resume_db.* TO 'resumeai'@'%';
GRANT ALL PRIVILEGES ON ai_db.* TO 'resumeai'@'%';
GRANT ALL PRIVILEGES ON template_db.* TO 'resumeai'@'%';
GRANT ALL PRIVILEGES ON payment_db.* TO 'resumeai'@'%';
GRANT ALL PRIVILEGES ON export_db.* TO 'resumeai'@'%';

FLUSH PRIVILEGES;