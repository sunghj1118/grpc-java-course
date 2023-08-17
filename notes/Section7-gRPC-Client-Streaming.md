[[Section 8 - gRPC BiDirectional Streaming]]


**LongGreet API:**
Multiple Greeting Requests - One Greeting Response

###### src/proto/greeting/greeting.proto

```protobuf
service GreetingService {  
	rpc greet(GreetingRequest) returns (GreetingResponse);  
	rpc greetManyTimes(GreetingRequest) returns (stream GreetingResponse);  
	rpc longGreet(stream GreetingRequest) returns (GreetingResponse);  
}
```

gradle에서 generate proto를 진행한다.
build/generated/.../grpc/greeting/GreetingServiceGrpc 파일이 생성된것을 확인할수 있다.

이때, longGreet의 기본적인 구현을 살펴보면 다음과 같이 GreetingRequest의 StreamObserver을 리턴하며, parameter로 GreetingResponse의 StreamObserver을 받습니다.

```java
default io.grpc.stub.StreamObserver<com.proto.greeting.GreetingRequest> longGreet(  
io.grpc.stub.StreamObserver<com.proto.greeting.GreetingResponse> responseObserver) {  
return io.grpc.stub.ServerCalls.asyncUnimplementedStreamingCall(getLongGreetMethod(), responseObserver);  
}
```


## Server Implementation
###### src/java/greeting/server/GreetingServerImpl
```java
@Override
public StreamObserver<GreetingRequest> longGreet(StreamObserver<GreetingResponse> responseObserver) {
	StringBuilder sb = new StringBuilder();

	return new StreamObserver<GreetingRequest>() {
		@Override
		public void onNext(GreetingRequest request) {
			sb.append("Hello ");
			sb.append(request.getFirstName());
			sb.append("!\n");
		}

		@Override
		public void onError(Throwable t) {
			responseObserver.onError(t);
		}

		@Override
		public void onCompleted() {
			responseObserver.onNext(GreetingResponse.newBuilder().setResult(sb.toString()).build());
		}
	};
}
```
StreamObserver은 세가지 메소드를 갖고 있는데 이들은 각각: onNext(), onError(), onCompleted()이다. 

onNext()는 request를 받을때마다 string에다 뒷붙히는 형식으로 구현했다.
onError()는 Throwable t를 그대로 responseObserver에게 주는 식으로 구현 되었다.
onCompleted()는 String Builder가 완성된 결과를 build 한다.

## Client Implementation
###### src/java/greeting/client/GreetingClient.java
```java
case "long_greet": doLongGreet(channel); break;
```
switch cases에 위와 같이 long_greet에 대한 case를 추가하고,
이번 경우에는 Streaming Client이기 때문에 Asynchronous stub를 정의해야 한다. 

```java
private static void doLongGreet(ManagedChannel channel) {
        System.out.println("Enter doLongGreet");
        GreetingServiceGrpc.GreetingServiceStub stub = GreetingServiceGrpc.newStub(channel);

        List<String> names = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);
    }
```

이때, CountDownLatch라는게 등장하는데, CountDownLatch의 값이 1으로 설정돼있으며, 0이 될때까지 기다린다. 
이걸 사용하는 이유는, 서버로부터 Response가 올 수 있는데 언제 올지 모르기 때문에 Response가 올때까지 기다리는것이다. 

