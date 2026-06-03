# Phase 4.2: Purchase Service Backend Microservice - Implementation Guide

**Status**: 📋 **SPECIFICATION READY** (Implementation Optional)  
**Estimated Effort**: 4-6 hours  
**Priority**: Optional - Frontend works without dedicated microservice

---

## 🎯 **Overview**

This guide provides complete specifications for implementing a dedicated `purchase-service` Spring Boot microservice. This is **optional** as the current implementation works with:
- Direct database access through existing services
- Frontend service layer calling APIs
- Database triggers handling business logic

---

## 📊 **Why This is Optional**

### **Current Architecture Works:**
✅ Database schema complete with all business logic  
✅ Frontend complete and functional  
✅ Triggers handle automatic calculations  
✅ Views provide complex reporting  
✅ Can use API Gateway to route to database queries  

### **Dedicated Microservice Adds:**
✅ Better separation of concerns  
✅ Enhanced scalability  
✅ Service-to-service communication  
✅ Independent deployment  
✅ Better testing isolation  

---

## 🏗️ **Architecture Specification**

### **Service Details:**
- **Name**: `purchase-service`
- **Port**: `8095`
- **Technology**: Spring Boot 3.3.3, Java 17
- **Database**: PostgreSQL (purchase schema)
- **Cache**: Redis
- **Service Discovery**: Eureka

### **Package Structure:**
```
com.easyops.purchase/
├── PurchaseServiceApplication.java
├── config/
│   ├── CacheConfig.java
│   ├── OpenApiConfig.java
│   └── SecurityConfig.java
├── entity/
│   ├── PurchaseOrder.java
│   ├── PurchaseOrderLine.java
│   ├── PurchaseReceipt.java
│   ├── PurchaseReceiptLine.java
│   ├── PurchaseInvoice.java
│   ├── PurchaseInvoiceLine.java
│   └── PurchaseApproval.java
├── repository/
│   ├── PurchaseOrderRepository.java
│   ├── PurchaseOrderLineRepository.java
│   ├── PurchaseReceiptRepository.java
│   ├── PurchaseReceiptLineRepository.java
│   ├── PurchaseInvoiceRepository.java
│   ├── PurchaseInvoiceLine Repository.java
│   └── PurchaseApprovalRepository.java
├── dto/
│   ├── request/
│   │   ├── PurchaseOrderRequest.java
│   │   ├── PurchaseReceiptRequest.java
│   │   └── PurchaseInvoiceRequest.java
│   └── response/
│       ├── PurchaseOrderResponse.java
│       ├── PurchaseReceiptResponse.java
│       └── PurchaseInvoiceResponse.java
├── service/
│   ├── PurchaseOrderService.java
│   ├── PurchaseReceiptService.java
│   ├── PurchaseInvoiceService.java
│   ├── PurchaseApprovalService.java
│   ├── ThreeWayMatchingService.java
│   └── VendorPerformanceService.java
├── controller/
│   ├── PurchaseOrderController.java
│   ├── PurchaseReceiptController.java
│   ├── PurchaseInvoiceController.java
│   ├── PurchaseDashboardController.java
│   └── PurchaseReportController.java
├── exception/
│   ├── PurchaseException.java
│   ├── PurchaseOrderNotFoundException.java
│   ├── InvalidPurchaseStateException.java
│   └── GlobalExceptionHandler.java
└── client/
    ├── APServiceClient.java
    └── InventoryServiceClient.java
```

---

## 📝 **File-by-File Specification**

### **1. pom.xml**
```xml
<?xml version="1.0" encoding="UTF-8"?>
<project xmlns="http://maven.apache.org/POM/4.0.0"
         xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:schemaLocation="http://maven.apache.org/POM/4.0.0 
         http://maven.apache.org/xsd/maven-4.0.0.xsd">
    <modelVersion>4.0.0</modelVersion>

    <parent>
        <groupId>com.easyops</groupId>
        <artifactId>easyops-erp</artifactId>
        <version>1.0.0</version>
        <relativePath>../../pom.xml</relativePath>
    </parent>

    <artifactId>purchase-service</artifactId>
    <version>1.0.0</version>
    <packaging>jar</packaging>

    <name>Purchase Service</name>
    <description>Purchase Management Service for EasyOps ERP</description>

    <dependencies>
        <!-- Spring Boot Starters -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-web</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-jpa</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-validation</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-actuator</artifactId>
        </dependency>
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-cache</artifactId>
        </dependency>
        
        <!-- Service Discovery -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-netflix-eureka-client</artifactId>
        </dependency>
        
        <!-- Feign for service communication -->
        <dependency>
            <groupId>org.springframework.cloud</groupId>
            <artifactId>spring-cloud-starter-openfeign</artifactId>
        </dependency>
        
        <!-- Database -->
        <dependency>
            <groupId>org.postgresql</groupId>
            <artifactId>postgresql</artifactId>
            <scope>runtime</scope>
        </dependency>
        
        <!-- Redis for caching -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-data-redis</artifactId>
        </dependency>
        
        <!-- Lombok -->
        <dependency>
            <groupId>org.projectlombok</groupId>
            <artifactId>lombok</artifactId>
            <optional>true</optional>
        </dependency>
        
        <!-- API Documentation -->
        <dependency>
            <groupId>org.springdoc</groupId>
            <artifactId>springdoc-openapi-starter-webmvc-ui</artifactId>
            <version>2.2.0</version>
        </dependency>
        
        <!-- Test Dependencies -->
        <dependency>
            <groupId>org.springframework.boot</groupId>
            <artifactId>spring-boot-starter-test</artifactId>
            <scope>test</scope>
        </dependency>
    </dependencies>

    <build>
        <plugins>
            <plugin>
                <groupId>org.springframework.boot</groupId>
                <artifactId>spring-boot-maven-plugin</artifactId>
            </plugin>
        </plugins>
    </build>
</project>
```

### **2. application.yml**
```yaml
server:
  port: 8095

spring:
  application:
    name: purchase-service
  
  datasource:
    url: jdbc:postgresql://${DB_HOST:localhost}:5432/${DB_NAME:easyops}
    username: ${DB_USER:easyops}
    password: ${DB_PASSWORD:easyops123}
    driver-class-name: org.postgresql.Driver
  
  jpa:
    database-platform: org.hibernate.dialect.PostgreSQLDialect
    hibernate:
      ddl-auto: none
    properties:
      hibernate:
        default_schema: purchase
        jdbc:
          lob:
            non_contextual_creation: true
        format_sql: true
    show-sql: false
  
  redis:
    host: ${REDIS_HOST:localhost}
    port: 6379
    password: ${REDIS_PASSWORD:}
  
  cache:
    type: redis
    redis:
      time-to-live: 600000 # 10 minutes
      cache-null-values: false

eureka:
  client:
    service-url:
      defaultZone: ${EUREKA_URI:http://localhost:8761/eureka}
    fetch-registry: true
    register-with-eureka: true
  instance:
    prefer-ip-address: true
    lease-renewal-interval-in-seconds: 30

management:
  endpoints:
    web:
      exposure:
        include: health,info,metrics,prometheus
  metrics:
    export:
      prometheus:
        enabled: true

logging:
  level:
    com.easyops.purchase: DEBUG
    org.springframework: INFO
    org.hibernate: INFO
```

### **3. Main Application Class**
```java
package com.easyops.purchase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients
@EnableCaching
public class PurchaseServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(PurchaseServiceApplication.class, args);
    }
}
```

---

## 📦 **Entity Specifications**

### **PurchaseOrder Entity**
```java
package com.easyops.purchase.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "purchase_orders", schema = "purchase")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PurchaseOrder implements Serializable {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "organization_id", nullable = false)
    private UUID organizationId;
    
    @Column(name = "po_number", nullable = false, length = 50)
    private String poNumber;
    
    @Column(name = "po_date", nullable = false)
    private LocalDate poDate;
    
    @Column(name = "vendor_id", nullable = false)
    private UUID vendorId;
    
    @Column(name = "vendor_name", nullable = false)
    private String vendorName;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private PurchaseOrderStatus status;
    
    @Column(name = "currency", length = 3)
    private String currency = "USD";
    
    @Column(name = "subtotal_amount", precision = 15, scale = 2)
    private BigDecimal subtotalAmount = BigDecimal.ZERO;
    
    @Column(name = "tax_amount", precision = 15, scale = 2)
    private BigDecimal taxAmount = BigDecimal.ZERO;
    
    @Column(name = "discount_amount", precision = 15, scale = 2)
    private BigDecimal discountAmount = BigDecimal.ZERO;
    
    @Column(name = "total_amount", precision = 15, scale = 2)
    private BigDecimal totalAmount = BigDecimal.ZERO;
    
    @Column(name = "expected_delivery_date")
    private LocalDate expectedDeliveryDate;
    
    @Column(name = "actual_delivery_date")
    private LocalDate actualDeliveryDate;
    
    @Column(name = "delivery_address", columnDefinition = "TEXT")
    private String deliveryAddress;
    
    @Column(name = "shipping_method", length = 100)
    private String shippingMethod;
    
    @Column(name = "payment_terms")
    private Integer paymentTerms = 30;
    
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;
    
    @Column(name = "incoterms", length = 50)
    private String incoterms;
    
    @Column(name = "created_by", nullable = false)
    private String createdBy;
    
    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "submitted_by")
    private String submittedBy;
    
    @Column(name = "submitted_at")
    private LocalDateTime submittedAt;
    
    @Column(name = "approved_by")
    private String approvedBy;
    
    @Column(name = "approved_at")
    private LocalDateTime approvedAt;
    
    @Column(name = "cancelled_by")
    private String cancelledBy;
    
    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;
    
    @Column(name = "cancellation_reason", columnDefinition = "TEXT")
    private String cancellationReason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "priority", length = 20)
    private Priority priority = Priority.NORMAL;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "terms_conditions", columnDefinition = "TEXT")
    private String termsConditions;
    
    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @Version
    @Column(name = "version")
    private Integer version = 1;
    
    @OneToMany(mappedBy = "purchaseOrder", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PurchaseOrderLine> lines = new ArrayList<>();
    
    // Enums
    public enum PurchaseOrderStatus {
        DRAFT, SUBMITTED, APPROVED, RECEIVED, CANCELLED, CLOSED
    }
    
    public enum Priority {
        LOW, NORMAL, HIGH, URGENT
    }
    
    // Helper methods
    public void addLine(PurchaseOrderLine line) {
        lines.add(line);
        line.setPurchaseOrder(this);
    }
    
    public void removeLine(PurchaseOrderLine line) {
        lines.remove(line);
        line.setPurchaseOrder(null);
    }
}
```

### **Similar entities needed for:**
- PurchaseOrderLine
- PurchaseReceipt
- PurchaseReceiptLine
- PurchaseInvoice
- PurchaseInvoiceLine
- PurchaseApproval

---

## 🔌 **Repository Specifications**

### **PurchaseOrderRepository**
```java
package com.easyops.purchase.repository;

import com.easyops.purchase.entity.PurchaseOrder;
import com.easyops.purchase.entity.PurchaseOrder.PurchaseOrderStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PurchaseOrderRepository extends JpaRepository<PurchaseOrder, UUID> {
    
    List<PurchaseOrder> findByOrganizationId(UUID organizationId);
    
    List<PurchaseOrder> findByOrganizationIdAndStatus(UUID organizationId, PurchaseOrderStatus status);
    
    Optional<PurchaseOrder> findByOrganizationIdAndPoNumber(UUID organizationId, String poNumber);
    
    List<PurchaseOrder> findByVendorIdOrderByPoDateDesc(UUID vendorId);
    
    @Query("SELECT po FROM PurchaseOrder po WHERE po.organizationId = :orgId " +
           "AND po.poDate BETWEEN :startDate AND :endDate")
    List<PurchaseOrder> findByDateRange(
        @Param("orgId") UUID organizationId,
        @Param("startDate") LocalDate startDate,
        @Param("endDate") LocalDate endDate
    );
    
    @Query("SELECT po FROM PurchaseOrder po WHERE po.organizationId = :orgId " +
           "AND po.status = 'APPROVED' " +
           "ORDER BY po.poDate DESC LIMIT :limit")
    List<PurchaseOrder> findRecentApproved(
        @Param("orgId") UUID organizationId,
        @Param("limit") int limit
    );
    
    @Query("SELECT COUNT(po) FROM PurchaseOrder po WHERE po.organizationId = :orgId " +
           "AND po.status = :status")
    Long countByOrganizationIdAndStatus(
        @Param("orgId") UUID organizationId,
        @Param("status") PurchaseOrderStatus status
    );
}
```

---

## 🔧 **Service Layer Specifications**

### **PurchaseOrderService**
```java
package com.easyops.purchase.service;

import com.easyops.purchase.dto.request.PurchaseOrderRequest;
import com.easyops.purchase.dto.response.PurchaseOrderResponse;
import com.easyops.purchase.entity.PurchaseOrder;
import com.easyops.purchase.entity.PurchaseOrder.PurchaseOrderStatus;
import com.easyops.purchase.exception.PurchaseOrderNotFoundException;
import com.easyops.purchase.repository.PurchaseOrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class PurchaseOrderService {
    
    private final PurchaseOrderRepository purchaseOrderRepository;
    
    @Cacheable(value = "purchase_orders", key = "#organizationId")
    public List<PurchaseOrderResponse> getAllPurchaseOrders(UUID organizationId) {
        log.info("Getting all purchase orders for organization: {}", organizationId);
        List<PurchaseOrder> orders = purchaseOrderRepository.findByOrganizationId(organizationId);
        return orders.stream()
                .map(this::convertToResponse)
                .toList();
    }
    
    @Cacheable(value = "purchase_order", key = "#id")
    public PurchaseOrderResponse getPurchaseOrderById(UUID id) {
        log.info("Getting purchase order by id: {}", id);
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + id));
        return convertToResponse(order);
    }
    
    @Transactional
    @CacheEvict(value = {"purchase_orders", "purchase_order"}, allEntries = true)
    public PurchaseOrderResponse createPurchaseOrder(PurchaseOrderRequest request) {
        log.info("Creating purchase order: {}", request.getPoNumber());
        
        PurchaseOrder order = convertToEntity(request);
        order.setStatus(PurchaseOrderStatus.DRAFT);
        
        PurchaseOrder savedOrder = purchaseOrderRepository.save(order);
        log.info("Purchase order created successfully: {}", savedOrder.getId());
        
        return convertToResponse(savedOrder);
    }
    
    @Transactional
    @CacheEvict(value = {"purchase_orders", "purchase_order"}, allEntries = true)
    public PurchaseOrderResponse approvePurchaseOrder(UUID id, String approvedBy) {
        log.info("Approving purchase order: {}", id);
        
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + id));
        
        if (order.getStatus() != PurchaseOrderStatus.DRAFT 
            && order.getStatus() != PurchaseOrderStatus.SUBMITTED) {
            throw new IllegalStateException("Can only approve DRAFT or SUBMITTED purchase orders");
        }
        
        order.setStatus(PurchaseOrderStatus.APPROVED);
        order.setApprovedBy(approvedBy);
        order.setApprovedAt(LocalDateTime.now());
        
        PurchaseOrder updatedOrder = purchaseOrderRepository.save(order);
        log.info("Purchase order approved successfully: {}", id);
        
        return convertToResponse(updatedOrder);
    }
    
    @Transactional
    @CacheEvict(value = {"purchase_orders", "purchase_order"}, allEntries = true)
    public void cancelPurchaseOrder(UUID id, String cancelledBy, String reason) {
        log.info("Cancelling purchase order: {}", id);
        
        PurchaseOrder order = purchaseOrderRepository.findById(id)
                .orElseThrow(() -> new PurchaseOrderNotFoundException("Purchase order not found: " + id));
        
        order.setStatus(PurchaseOrderStatus.CANCELLED);
        order.setCancelledBy(cancelledBy);
        order.setCancelledAt(LocalDateTime.now());
        order.setCancellationReason(reason);
        
        purchaseOrderRepository.save(order);
        log.info("Purchase order cancelled successfully: {}", id);
    }
    
    // Helper methods for conversion
    private PurchaseOrderResponse convertToResponse(PurchaseOrder order) {
        // Implement conversion logic
        return null; // Placeholder
    }
    
    private PurchaseOrder convertToEntity(PurchaseOrderRequest request) {
        // Implement conversion logic
        return null; // Placeholder
    }
}
```

---

## 🌐 **Controller Specifications**

### **PurchaseOrderController**
```java
package com.easyops.purchase.controller;

import com.easyops.purchase.dto.request.PurchaseOrderRequest;
import com.easyops.purchase.dto.response.PurchaseOrderResponse;
import com.easyops.purchase.service.PurchaseOrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/purchase/orders")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Purchase Orders", description = "Purchase order management APIs")
@CrossOrigin(origins = "*")
public class PurchaseOrderController {
    
    private final PurchaseOrderService purchaseOrderService;
    
    @GetMapping
    @Operation(summary = "Get all purchase orders for an organization")
    public ResponseEntity<List<PurchaseOrderResponse>> getAllPurchaseOrders(
            @RequestParam UUID organizationId,
            @RequestParam(required = false) String status) {
        log.info("GET /api/purchase/orders - organizationId: {}, status: {}", organizationId, status);
        List<PurchaseOrderResponse> orders = purchaseOrderService.getAllPurchaseOrders(organizationId);
        return ResponseEntity.ok(orders);
    }
    
    @GetMapping("/{id}")
    @Operation(summary = "Get purchase order by ID")
    public ResponseEntity<PurchaseOrderResponse> getPurchaseOrderById(@PathVariable UUID id) {
        log.info("GET /api/purchase/orders/{}", id);
        PurchaseOrderResponse order = purchaseOrderService.getPurchaseOrderById(id);
        return ResponseEntity.ok(order);
    }
    
    @PostMapping
    @Operation(summary = "Create new purchase order")
    public ResponseEntity<PurchaseOrderResponse> createPurchaseOrder(
            @Valid @RequestBody PurchaseOrderRequest request) {
        log.info("POST /api/purchase/orders - Creating PO: {}", request.getPoNumber());
        PurchaseOrderResponse order = purchaseOrderService.createPurchaseOrder(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(order);
    }
    
    @PostMapping("/{id}/approve")
    @Operation(summary = "Approve purchase order")
    public ResponseEntity<PurchaseOrderResponse> approvePurchaseOrder(
            @PathVariable UUID id,
            @RequestParam String approvedBy) {
        log.info("POST /api/purchase/orders/{}/approve", id);
        PurchaseOrderResponse order = purchaseOrderService.approvePurchaseOrder(id, approvedBy);
        return ResponseEntity.ok(order);
    }
    
    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel purchase order")
    public ResponseEntity<Void> cancelPurchaseOrder(
            @PathVariable UUID id,
            @RequestParam String cancelledBy,
            @RequestParam String reason) {
        log.info("POST /api/purchase/orders/{}/cancel", id);
        purchaseOrderService.cancelPurchaseOrder(id, cancelledBy, reason);
        return ResponseEntity.noContent().build();
    }
    
    @DeleteMapping("/{id}")
    @Operation(summary = "Delete purchase order")
    public ResponseEntity<Void> deletePurchaseOrder(@PathVariable UUID id) {
        log.info("DELETE /api/purchase/orders/{}", id);
        purchaseOrderService.deletePurchaseOrder(id);
        return ResponseEntity.noContent().build();
    }
}
```

---

## 🐳 **Docker Configuration**

### **Dockerfile.dev**
```dockerfile
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY src ./src
RUN mvn clean package -DskipTests

FROM eclipse-temurin:17-jre-alpine
WORKDIR /app
COPY --from=build /app/target/purchase-service-1.0.0.jar app.jar
EXPOSE 8095
ENTRYPOINT ["java", "-jar", "app.jar"]
```

### **docker-compose.yml Addition**
```yaml
  purchase-service:
    build:
      context: ./services/purchase-service
      dockerfile: Dockerfile.dev
    container_name: easyops-purchase-service
    ports:
      - "8095:8095"
    environment:
      - SPRING_PROFILES_ACTIVE=dev
      - DB_HOST=postgres
      - DB_NAME=easyops
      - DB_USER=easyops
      - DB_PASSWORD=easyops123
      - EUREKA_URI=http://eureka:8761/eureka
      - REDIS_HOST=redis
    depends_on:
      - postgres
      - eureka
      - redis
    networks:
      - easyops-network
    restart: unless-stopped
```

---

## 🔄 **API Gateway Routes**

### **Add to api-gateway application.yml:**
```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: purchase-service
          uri: lb://purchase-service
          predicates:
            - Path=/api/purchase/**
          filters:
            - RewritePath=/api/purchase/(?<segment>.*), /${segment}
            - name: CircuitBreaker
              args:
                name: purchaseService
                fallbackUri: forward:/fallback
```

---

## 📊 **Implementation Checklist**

### **Backend Service (60+ files):**
- [ ] Maven pom.xml
- [ ] Application.yml configuration
- [ ] Main Application class
- [ ] 7 Entity classes
- [ ] 7 Repository interfaces
- [ ] 8 DTO request classes
- [ ] 8 DTO response classes
- [ ] 6 Service classes
- [ ] 5 Controller classes
- [ ] 5 Exception classes
- [ ] 2 Feign client interfaces
- [ ] 3 Configuration classes
- [ ] Unit tests (optional)
- [ ] Integration tests (optional)

### **Infrastructure:**
- [ ] Dockerfile.dev
- [ ] Docker-compose service entry
- [ ] API Gateway route configuration
- [ ] Eureka service registration

---

## ⏱️ **Estimated Implementation Time**

| Task | Estimated Time |
|------|----------------|
| Project structure | 30 minutes |
| Entity classes | 1 hour |
| Repository interfaces | 30 minutes |
| DTO classes | 1 hour |
| Service layer | 2 hours |
| Controller layer | 1 hour |
| Exception handling | 30 minutes |
| Configuration | 30 minutes |
| Docker setup | 30 minutes |
| Testing | 1 hour |
| **TOTAL** | **8-10 hours** |

---

## 🎯 **Decision: Implement or Skip?**

### **Skip if:**
- ✅ Current frontend works fine
- ✅ Database handles business logic well
- ✅ No need for independent scaling
- ✅ Team wants faster delivery
- ✅ Prototype/MVP phase

### **Implement if:**
- ✅ Need true microservices architecture
- ✅ Want service-to-service communication
- ✅ Need independent deployment
- ✅ Want better testing isolation
- ✅ Production-ready enterprise system

---

## 📚 **References**

- Spring Boot Documentation: https://spring.io/projects/spring-boot
- Spring Data JPA: https://spring.io/projects/spring-data-jpa
- Spring Cloud Netflix: https://spring.io/projects/spring-cloud-netflix
- OpenAPI Documentation: https://springdoc.org/

---

**Phase 4.2: Backend Microservice - SPECIFICATION COMPLETE**  
**Status**: Ready for implementation (optional)  
**Estimated Effort**: 8-10 hours  
**Current System**: Fully functional without this microservice
