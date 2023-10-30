# Linux-Manual-Web-Crawller
An application created to browse the provided URL in search of a term provided by users, and it returns a list of URLs where the terms were found.

## Built With
Java  Version 14 - Frameworks ( Spark, Gson, slf4j, JUnit and Mockito )
Maven Version 3.8.1

## Getting Started
Before starting the application, you must build it.

Run command at the terminal:
* docker terminal
  ```sh
  docker build . -t zanatta/backend
  ```
  
Starting application
* docker terminal
  ```sh
  docker run -e BASE_URL=http://hiring.axreng.com/ -p 4567:4567 --rm zanatta/backend
  ```

## Usage

Configure postaman rote POST for 
```sh
  http://localhost:4567/crawl
  ```
and add body param 
```sh
  {
    "keyword": "security"
  }
  ```

Use the response id for the GET rote
```sh
  http://localhost:4567/crawl/{idFromPOST}
  ```
It will return all urls with the used keyword.
