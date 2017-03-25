Just a sketch for async-REST with Spring.

Run main application (`AsyncRestApplication`), and open one of the following URL's in browser:
* regular, blocking behaviour
```
http://localhost:8880/blocking/<SOME_STRING>
```
* async caller but expensive resource not decoupled - solved with `Callable`
```
http://localhost:8880/callable/<SOME_STRING>
```
* same as above, `DeferredResult` instead of `Callable`
```
http://localhost:8880/deferred/<SOME_STRING>
```
* fully async. Fakes a real messaging-system (JMS, Kafka,...)
```
http://localhost:8880/fully-async/<SOME_STRING>
```
