package com.miskoscode.customer;

import com.miskoscode.amqp.RabbitMQMessageProducer;
import com.miskoscode.clients.fraud.FraudCheckResponse;
import com.miskoscode.clients.fraud.FraudClient;
import com.miskoscode.clients.notification.NotificationClient;
import com.miskoscode.clients.notification.NotificationRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public record CustomerService(
        CustomerRepository customerRepository,
        RestTemplate restTemplate,
        FraudClient fraudClient,
        NotificationClient notificationClient,
        RabbitMQMessageProducer producer
) {
    public void registerCustomer(CustomerRegistrationRequest request) {
        Customer customer = Customer.builder()
                .firstName(request.firstName())
                .lastName(request.lastName())
                .email(request.email())
                .build();
        //todo: email if valid
        //todo: if email not taken
        customerRepository.saveAndFlush(customer);
        //todo: check if fraudster

        //with eureka
//        FraudCheckResponse fraudCheckResponse = restTemplate.getForObject(
//                "http://FRAUD/api/v1/fraud-check/{customerId}",
//                FraudCheckResponse.class,
//                customer.getId()
//        );
        // with open feign
        FraudCheckResponse fraudCheckResponse = fraudClient.isFraudster(customer.getId());

        if(fraudCheckResponse.isFraudster()){
            throw new IllegalStateException("fraudster");
        }
        // end todo : check if fraudster

        //todo: send notification
        // todo: make it async. i.e add to queue
        NotificationRequest notificationRequest = new NotificationRequest(
                customer.getId(),
                customer.getEmail(),
                String.format("Hi %s, welcome to Amigoscode...",
                        customer.getFirstName())
        );
        // old way
        //        notificationClient.sendNotification(
        //                notificationRequest
        //        );

        producer.publish(
                notificationRequest,
                "internal.exchange",
                "internal.notification-key"
        );
    }
}
