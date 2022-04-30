# FreeRCT Website

This is the new official website for [FreeRCT](https://github.com/FreeRCT/FreeRCT/).

It is written in Java 17 using the Spring Boot framework.

## Building

To build the project, you will need a Java compiler (version 17) and Maven:
```
sudo apt install openjdk-17-* maven
```

To compile and run the website locally, type:
```
git clone git@github.com:FreeRCT/FreeRCT-website.git
cd FreeRCT-website
./mvnw spring-boot:run
```
Then open your web browser and visit http://localhost:8080/.

## Configuration

Add a `config` file in the toplevel directory of the git checkout.
It's a simple ini-style file with key-value pairs, e.g.:
```
server.port = 8080
server.servlet.session.timeout = 15m
```

The config file consists of two sections which are separated by a line consisting only of '~' characters.

The first section allows you to override Spring Boot's default properties.
See [the docs](https://docs.spring.io/spring-boot/docs/current/reference/html/application-properties.html) for available properties.

The second section sets variables for the server implementation. The server expects the following variables:

Key                  | Description                                                                      | Example
-------------------- | -------------------------------------------------------------------------------- | ------------------
`freerct`            | Path to the FreeRCT git checkout                                                 | `/var/freerct`
`databasehost`       | IP address of the MySQL server                                                   | `127.0.0.1`
`databaseport`       | Port number of the MySQL server                                                  | `3306`
`databasename`       | Name of the database                                                             | `freerct_db`
`databaseuser`       | The database user                                                                | `freerct`
`databasepassword`   | Password for the database user                                                   | `123456`
`printmail`          | Optional debugging option. If `true`, e-mails will be echoed to standard output. | `true`

## Database

The server connects to a MySQL database.
The user you specified in the config file must have `SELECT`, `INSERT`, `UPDATE`, and `DELETE` access to the database.
The database contains the following tables:

### `users`

Each row represents a registered website user.

Column               | Type             | Description
-------------------- | ---------------- | -----------------------------------------------------------------------------------------------------------------------------
id                   | int              | Unique user ID.
username             | varchar          | The user's displayed name.
email                | varchar          | E-mail address to send notifications.
password             | varchar          | The hash of the user's password.
joined               | datetime         | The time and date the user registered on the website.
state                | int              | The user state, see below.
activation_token     | varchar          | The activation or password resetting token, if applicable.
activation_expire    | datetime         | Expiration time for the activation_token.

Valid user states (the constants are hardcoded) are:

Constant | Description
-------- | -------------------------
0        | Normal user
1        | Administrator
2        | Moderator
3        | Deactivated account
4        | Awaiting activation

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
views                | int              | Topic view counter.

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

### `noticetypes`

Each row represents a type of e-mail notification.

Column               | Type             | Description
-------------------- | ---------------- | ----------------------------------------------------------
id                   | int              | The notice type ID.
slug                 | varchar          | The internal name.
name                 | varchar          | The human-readable name.
description          | varchar          | The human-readable description.
default_enable       | int              | Whether the notice type is enabled by default (1 or 0).

### `notification_settings`

Each row represents a user's setting for a specific notification type.

Column               | Type             | Description
-------------------- | ---------------- | ----------------------------------------------------------
id                   | int              | Table key.
user                 | int              | The user's ID.
notice               | int              | The notice type ID.
state                | int              | 1 if the notice type is enabled, 0 if it's disabled.

### `subscriptions`

Each row represents a user's subscription to a specific topic.

Column               | Type             | Description
-------------------- | ---------------- | ----------------------------------------------------------
id                   | int              | Table key.
user                 | int              | The user's ID.
topic                | int              | The topic's ID.

### *ToDo add wiki tables*

### Table Schema
```sql
-- Delete all current tables in the correct order.
-- Do this only if you don't care about data loss!!!
drop table if exists notification_settings;
drop table if exists noticetypes;
drop table if exists subscriptions;
drop table if exists posts;
drop table if exists topics;
drop table if exists forums;
drop table if exists news;
drop table if exists messages;
drop table if exists users;

-- Create the new tables.
create table users (
	id                int          not null  primary key auto_increment,
	username          varchar(255) not null,
	email             varchar(255) not null,
	password          varchar(255) not null,
	joined            datetime     not null  default current_timestamp,
	state             int          not null  default 4,
	activation_token  varchar(255)           default null,
	activation_expire datetime               default null
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
	views int          not null default 0,
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

create table noticetypes (
	id             int          not null primary key auto_increment,
	slug           varchar(255) not null,
	name           varchar(255) not null,
	description    varchar(255) not null,
	default_enable int          not null
);

create table notification_settings (
	id     int not null primary key auto_increment,
	user   int not null,
	notice int not null,
	state  int not null,
	foreign key fk_user  (user  ) references users       (id) on delete cascade on update cascade,
	foreign key fk_notice(notice) references noticetypes (id) on delete cascade on update cascade
);

create table subscriptions (
	id    int not null primary key auto_increment,
	user  int not null,
	topic int not null,
	foreign key fk_user (user ) references users  (id) on delete cascade on update cascade,
	foreign key fk_topic(topic) references topics (id) on delete cascade on update cascade
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
