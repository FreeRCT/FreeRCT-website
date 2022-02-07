# FreeRCT Website

This is the new, under-development, not-yet-official website for [FreeRCT](https://github.com/FreeRCT/FreeRCT/).

It is written in Java 17 using the Spring Boot framework.

## Building

To build the project, you will need a Java compiler (version 17) and Maven:
```
sudo apt install openjdk-17-* maven
```

To compile and run the website locally, type:
```
git clone git@github.com:Noordfrees/freerct-website.git
cd freerct-website
./mvnw spring-boot:run
```
Then open your web browser and visit http://localhost:8080/.

## Configuration

Add a `config` file in the toplevel directory of the git checkout.
It's a simple ini-style file with key-value pairs, e.g.:
```
server.port = 9001
```

The config file consists of two sections which are separated by a line consisting only of '~' characters.

The first section allows you to override Spring Boot's default properties.
See [the docs](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html) for available properties.

The second section sets variables for the server implementation. The server expects the following variables:

Key                  | Description                         | Example
-------------------- | ----------------------------------- | ------------------
`databasehost`       | IP address of the MySQL server      | `127.0.0.1`
`databaseport`       | Port number of the MySQL server     | `3306`
`databasename`       | Name of the database                | `freerct_db`
`databaseuser`       | The database user                   | `freerct`
`databasepassword`   | Password for the database user      | `123456`

## Database

The server connects to a MySQL database.
The user you specified in the config file must have `SELECT`, `INSERT`, and `DELETE` access to the database.
The database contains the following tables:

### `users`

Each row represents a registered website user.

Column               | Type             | Description
-------------------- | ---------------- | -----------------------------------------------------------------------------------------
id                   | int              | Unique user ID.
username             | varchar          | The user's displayed name.
email                | varchar          | E-mail address to send notifications.
password             | varchar          | The hash of the user's password.
joined               | datetime         | The time and date the user registered on the website.
state                | int              | 0 for normal users, 1 for administrators, 2 for moderators, 3 for deactivated accounts.

### `news`

Each row represents a news announcement.

Column               | Type             | Description
-------------------- | ---------------- | ----------------------------------------------------------
id                   | int              | Unique news item ID.
author               | int              | ID of the user who created the news item.
timestamp            | datetime         | The time and date the item was posted.
slug                 | varchar          | The news item's URL title.
title                | varchar          | The news item's heading.
body                 | text             | The news item's content.

### `forums`

Each row represents a forum on the website.

Column               | Type             | Description
-------------------- | ---------------- | ----------------------------------------------------------
id                   | int              | Unique forum ID.
name                 | varchar          | Display name of the forum.
description          | varchar          | Long description of the forum's intended purpose.

### `topics`

Each row represents a topic in the forums.

Column               | Type             | Description
-------------------- | ---------------- | ----------------------------------------------------------
id                   | int              | Unique topic ID.
forum                | int              | The forum the topic belongs to.
name                 | varchar          | The title of the topic.

### `posts`

Each row represents a post under a forum topic.

Column               | Type             | Description
-------------------- | ---------------- | ----------------------------------------------------------
id                   | int              | Unique post ID.
topic                | int              | The topic the post belongs to.
user                 | int              | The user who posted the topic.
editor               | int              | The user who last edited the topic (may be null).
created              | datetime         | The time and date the post was created.
edited               | datetime         | The time and date the post was last edited (may be null).
body                 | text             | The content of the post.

### `messages`

Each row represents a private message sent from a user to another user.

Column               | Type             | Description
-------------------- | ---------------- | ----------------------------------------------------------
id                   | int              | Unique message ID.
sender               | int              | The user who sent the message.
recipient            | int              | The user who received the message.
subject              | varchar          | The subject of the message.
body                 | text             | The content of the message.
timestamp            | datetime         | The time and date the message was sent.
state                | int              | One of 0 (unread), 1 (read), or 2 (deleted).

### *ToDo add wiki tables*

### Table Schema
```sql
-- Delete all current tables in the correct order.
-- Do this only if you don't care about data loss!!!
drop table if exists posts;
drop table if exists topics;
drop table if exists forums;
drop table if exists news;
drop table if exists messages;
drop table if exists users;

-- Create the new tables.
create table users (
	id       int          not null  primary key auto_increment,
	username varchar(255) not null,
	email    varchar(255) not null,
	password varchar(255) not null,
	joined   datetime     not null  default current_timestamp,
	state    int          not null  default 0
);
create table news (
	id        int          not null primary key auto_increment,
	author    int          not null,
	timestamp datetime     not null default current_timestamp,
	slug      varchar(255) not null,
	title     varchar(255) not null,
	body      text         not null,
	foreign key fk_author (author) references users (id) on delete cascade on update cascade
);
create table forums (
	id          int          not null primary key auto_increment,
	name        varchar(255) not null,
	description varchar(255) not null
);
create table topics (
	id    int          not null primary key auto_increment,
	forum int          not null,
	name  varchar(255) not null,
	foreign key fk_forum (forum) references forums (id) on delete cascade on update cascade
);
create table posts (
	id      int      not null primary key auto_increment,
	topic   int      not null,
	user    int      not null,
	editor  int               default null,
	created datetime not null default current_timestamp,
	edited  datetime          default null,
	body    text     not null,
	foreign key fk_topic  (topic ) references topics (id) on delete cascade on update cascade,
	foreign key fk_user   (user  ) references users  (id) on delete cascade on update cascade,
	foreign key fk_editor (editor) references users  (id) on delete cascade on update cascade
);
create table messages (
	id        int          not null primary key auto_increment,
	sender    int          not null,
	recipient int          not null,
	subject   varchar(255) not null,
	body      text         not null,
	timestamp datetime     not null default current_timestamp,
	state     int          not null default 0,
	foreign key fk_sender    (sender   ) references users (id) on delete cascade on update cascade,
	foreign key fk_recipient (recipient) references users (id) on delete cascade on update cascade
);
```
