Run main application, open browser and open one of the following URL's:
* regular, blocking behaviour
`http://localhost:8880/blocking/<SOME_STRING>`
* async caller but expensive resource not decoupled - solved with `Callable`
`http://localhost:8880/callable/<SOME_STRING>`
* same as above, `DeferredResult` instead of Callable
`http://localhost:8880/deferred/<SOME_STRING>`
* fully async. Faked a real messaging-system (JMS, Kafka,...)
`http://localhost:8880/fully-async/<SOME_STRING>`
