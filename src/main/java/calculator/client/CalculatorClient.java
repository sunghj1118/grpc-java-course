package calculator.client;

import com.proto.calculator.*;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.stub.StreamObserver;

import java.sql.Time;
import java.util.Arrays;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CalculatorClient {
    private static void doSum(ManagedChannel channel){
        System.out.println("Enter doSum");
        CalculatorServiceGrpc.CalculatorServiceBlockingStub stub = CalculatorServiceGrpc.newBlockingStub(channel);
        SumResponse response = stub.sum(SumRequest.newBuilder().setFirstNumber(1).setSecondNumber(1).build());

        System.out.println("Sum 1 + 1 = " + response.getResult());
    }

    private static void doPrimes(ManagedChannel channel) {
        System.out.println("Enter doPrimes");
        CalculatorServiceGrpc.CalculatorServiceBlockingStub stub = CalculatorServiceGrpc.newBlockingStub(channel);

        stub.primes(PrimeRequest.newBuilder().setFirstNumber(567890).build()).forEachRemaining(response ->
                System.out.println(response.getPrimeFactor())
        );
    }

    private static void doAvg(ManagedChannel chanel) throws InterruptedException {
        System.out.println("Enter doAvg");
        CalculatorServiceGrpc.CalculatorServiceStub stub = CalculatorServiceGrpc.newStub(chanel);
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<AvgRequest> stream = stub.avg(new StreamObserver<AvgResponse>() {
            @Override
            public void onNext(AvgResponse value) {
                System.out.println("Avg = " + value.getResult());
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

        Arrays.asList(1, 2, 3, 4, 5, 6, 7, 8, 9, 10).forEach(number ->
                stream.onNext(AvgRequest.newBuilder().setNumber(number).build())
        );
        stream.onCompleted();
        latch.await(3, TimeUnit.SECONDS);
    }

    private static void doMax(ManagedChannel channel) throws InterruptedException {
        System.out.println("Enter doMax");
        CalculatorServiceGrpc.CalculatorServiceStub stub = CalculatorServiceGrpc.newStub(channel);
        CountDownLatch latch = new CountDownLatch(1);

        StreamObserver<MaxRequest> stream = stub.max(new StreamObserver<MaxResponse>() {
            @Override
            public void onNext(MaxResponse value) {
                System.out.println("Max = " + value.getMax());
            }

            @Override
            public void onError(Throwable t) {}

            @Override
            public void onCompleted() {
                latch.countDown();
            }
        });

        Arrays.asList(1, 5, 3, 6, 2, 20).forEach(number ->
                stream.onNext(MaxRequest.newBuilder().setNumber(number).build())
        );

        stream.onCompleted();
        latch.await(3, TimeUnit.SECONDS);
    }

    private static void doSqrt(ManagedChannel channel){
        System.out.println("Enter doSqrt");
        CalculatorServiceGrpc.CalculatorServiceBlockingStub stub = CalculatorServiceGrpc.newBlockingStub(channel);

        // no error
        SqrtResponse response = stub.sqrt(SqrtRequest.newBuilder().setNumber(25).build());
        System.out.println("Sqrt of 25 is " + response.getResponse());

        // error
        try {
            response = stub.sqrt(SqrtRequest.newBuilder().setNumber(-1).build());
            System.out.println("Sqrt of -1 is " + response.getResponse());
        } catch (RuntimeException e) {
            System.out.println("Got an exception for sqrt");
            e.printStackTrace();
        }
    }

    public static void main(String[] args) throws InterruptedException {
        if (args.length == 0){
            System.out.println("Need one argument to work");
            return;
        }

        ManagedChannel channel = ManagedChannelBuilder.forAddress("Localhost", 50052)
                .usePlaintext()
                .build();

        switch (args[0]) {
            case "sum": doSum(channel); break;
            case "primes": doPrimes(channel); break;
            case "avg": doAvg(channel); break;
            case "max": doMax(channel); break;
            case "sqrt": doSqrt(channel); break;
            default:
                System.out.println("Keyword invalid: " + args[0]);
        }
    }
}
