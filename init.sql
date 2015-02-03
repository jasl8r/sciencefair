

drop table adults;

drop table students;

CREATE TABLE adults (
id INT NOT NULL AUTO_INCREMENT,
  email VARCHAR(100),
   name VARCHAR(100),
    first_id INT,
     created_date DATETIME,
      updated_date DATETIME,
       paid INT,
        payment_choice varchar(20),
        photo_permission varchar(5),
        phone varchar(50),
       PRIMARY KEY (id));

CREATE TABLE students (id           INT NOT NULL AUTO_INCREMENT,
                       adult_id     INT,
                       student      VARCHAR(100),
                       school       VARCHAR(100),
                       grade        VARCHAR(2),
                       teacher      VARCHAR(100),
                       partner      VARCHAR(100),
                       title        VARCHAR(200),
                       description  TEXT,
                       created_date DATETIME,
                       updated_date DATETIME,

  PRIMARY KEY (id));
