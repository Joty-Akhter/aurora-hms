# Facility-aware routing to LIS/RIS/PACS

Order sets and clinical orders support an optional `facility_id`. When present, it can be used to route orders and result callbacks to facility-specific external systems (LIS, RIS, PACS).

## Facility validation

When creating an order set (or copying from one), if `facilityId` is provided it is validated via the optional `FacilityResolver` bean. By default a no-op implementation is used (any ID is accepted). To enforce validation against a facility master or hospital-service API:

1. Implement `com.easyops.hospitalclinicalorders.integration.FacilityResolver` (single method: `void validate(UUID facilityId)`; throw `IllegalArgumentException` if the facility does not exist or is not active).
2. Register your implementation as a Spring bean (e.g. `@Component` or `@Configuration`). If the default no-op should be disabled, provide your bean with `@Primary` or exclude `NoOpFacilityResolver` from component scan.
3. Your implementation can call hospital-service, a facility master API, or a local cache; this service does not depend on any specific facility API.

## Integration pattern

- **Config map**: Maintain a mapping `facility_id → external system endpoint` (e.g. in configuration or a facility master). Example:
  - `facility-001` → `https://lis-site-a.example.com/api/orders`
  - `facility-002` → `https://lis-site-b.example.com/api/orders`
- **Outbound**: When pushing orders to LIS/RIS (pull or push pattern), resolve the order’s `facility_id` (from the order set or denormalized on the order) and choose the target endpoint from this map.
- **Inbound**: The callback `POST /api/hospital-clinical-orders/orders/{id}/results` (result links) is facility-agnostic; the adapter that receives LIS/RIS callbacks can use the order’s `facility_id` for logging or routing if needed.
- **Adapter**: The actual HTTP/client adapter that calls LIS/RIS per facility belongs in the integration layer (e.g. a separate module or Spring `@Configuration` that reads the config map and exposes a `ResultSubmissionClient` per facility). This service only stores and returns `facility_id`; it does not implement the outbound call.

## List APIs and worklists

- **Order sets**: `GET /order-sets?facilityId={uuid}` returns only order sets for that facility.
- **Orders**: `GET /orders?facilityId={uuid}` returns only orders for that facility (using denormalized `clinical_orders.facility_id`).
- **Worklists**: `GET /worklists?facilityId={uuid}` returns only worklist items whose order belongs to an order set with that `facility_id`.

Use these filters for multi-tenant or multi-facility UIs and for routing downstream processing by facility.
