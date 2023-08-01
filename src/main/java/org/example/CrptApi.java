package org.example;

import com.google.gson.Gson;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.util.EntityUtils;

import java.util.*;
import java.util.concurrent.TimeUnit;

public class CrptApi {

    String TOKEN = System.getenv("TOKEN_CRPT");
    String API_ENDPOINT = "https://ismp.crpt.ru";

    int requestsCounter;

    private final TimeUnit timeUnit;
    private final int requestLimit;

    public CrptApi(TimeUnit timeUnit, int requestLimit) {
        this.timeUnit = timeUnit;
        if (requestLimit < 0) {
            throw new RuntimeException("Лимит запросов не должен равняться отрицательному числу!");
        }
        this.requestLimit = requestLimit;
        requestsCounter = requestLimit;
    }

    public String createDocumentForRussianProduct(Document document, String signature) {
        return postRequest(documentToJsonAndBase64(document), signature, "/api/v3/lk/documents/commissioning/contract/create");
    }

    public String documentToJsonAndBase64(Document document) {
        Gson gson = new Gson();
        Map<String, Object> map = new HashMap<>();
        map.put("doc_id", document.getDocId());
        map.put("doc_status", document.getDocStatus());
        map.put("doc_type", document.getDocType());
        map.put("owner_inn", document.getOwnerInn());
        map.put("participant_inn", document.getParticipantInn());
        map.put("producer_inn", document.getProducerInn());
        map.put("production_date", document.getProductionDate());
        map.put("production_type", document.getProductionType().name());
        map.put("reg_date", document.getRegDate());
        map.put("reg_number", document.getRegNumber());
        if (document.getDescription() != null) {
            map.put("description", gson.toJson(document.getDescription()));
        }
        if (document.getImportRequest() != null) {
            map.put("import_request", document.getImportRequest());
        }
        if (document.getProducts() != null) {
            List<Document.Product> products = document.getProducts();
            List<Map<String, Object>> productsToPut = new ArrayList<>();
            for (Document.Product product : products) {
                Map<String, Object> productToPut = new HashMap<>();
                productToPut.put("owner_inn", product.getOwnerInn());
                productToPut.put("producer_inn", product.getProducerInn());
                productToPut.put("tnved_code", product.getTnvedCode());
                if (product.getCertificateDocument() != null) {
                    productToPut.put("certificate_document", product.getCertificateDocument().name());
                }
                if (product.getCertificateDocumentDate() != null) {
                    productToPut.put("certificate_document_date", product.getCertificateDocumentDate());
                }
                if (product.getCertificateDocumentNumber() != null) {
                    productToPut.put("certificate_document_number", product.getCertificateDocumentNumber());
                }
                if (!document.getProductionDate().equals(product.getProductionDate())) {
                    productToPut.put("production_date", product.getProductionDate());
                } else {
                    productToPut.put("production_date", document.getProductionDate());
                }
                if (product.getUitCode() != null) {
                    productToPut.put("uit_code", product.getUitCode());
                } else if (product.getUituCode() != null) {
                    productToPut.put("uitu_code", product.getUituCode());
                } else {
                    throw new IllegalArgumentException("Хотя бы одно поле (uit_code/uitu_code) должно быть не пустым");
                }
                productsToPut.add(productToPut);
            }
            map.put("products", productsToPut);
        }
        String json = gson.toJson(map);
        byte[] encodedBytes = Base64.getEncoder().encode(json.getBytes());
        return Arrays.toString(encodedBytes);
    }

    private String postRequest(String base64, String signature, String apiUrl) {
        if (requestLimit != 0) {
            synchronized (this) {
                requestsCounter -= 1;
            }
        }
        if (requestsCounter < 0) {
            try {
                timeUnit.sleep(1);
                requestsCounter = requestLimit;
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        try {
            CloseableHttpClient client = HttpClientBuilder.create().build();
            HttpPost request = new HttpPost(API_ENDPOINT + apiUrl);

            Map<String, String> body = new HashMap<>();
            body.put("document_format", "MANUAL");
            body.put("product_document", base64);
            body.put("signature", signature);
            body.put("type", "LP_INTRODUCE_GOODS");

            Gson gson = new Gson();
            String json = gson.toJson(body);
            StringEntity stringEntity = new StringEntity(json);
            request.addHeader("content-type", "application/json");
            request.addHeader("Authorization", "Bearer " + TOKEN);
            request.setEntity(stringEntity);
            HttpResponse response = client.execute(request);
            HttpEntity entity = response.getEntity();
            String responseString = "";
            if (entity != null) {
                responseString = EntityUtils.toString(entity, "UTF-8");
            }
            client.close();

            return responseString;
        } catch (Exception ee) {
            ee.printStackTrace();
            return "";
        }
    }

    static class Document {

        private Description description;
        private final String docId;
        private final String docStatus;
        private final String docType;
        private String importRequest;
        private final String ownerInn;
        private final String participantInn;
        private final String producerInn;
        private final String productionDate;
        private final ProductionType productionType;
        private final String regDate;
        private final String regNumber;

        private List<Product> products;

        public Document(String docId, String docStatus, String docType, String ownerInn,
                        String participantInn, String producerInn, String productionDate,
                        ProductionType productionType, String regDate, String regNumber) {
            this.docId = docId;
            this.docStatus = docStatus;
            this.docType = docType;
            this.ownerInn = ownerInn;
            this.participantInn = participantInn;
            this.producerInn = producerInn;
            this.productionDate = productionDate;
            this.productionType = productionType;
            this.regDate = regDate;
            this.regNumber = regNumber;
        }

        public void setDescription(Description description) {
            this.description = description;
        }

        public void setImportRequest(String importRequest) {
            this.importRequest = importRequest;
        }

        public Description getDescription() {
            return description;
        }

        public String getDocId() {
            return docId;
        }

        public String getDocStatus() {
            return docStatus;
        }

        public String getDocType() {
            return docType;
        }

        public String getImportRequest() {
            return importRequest;
        }

        public String getOwnerInn() {
            return ownerInn;
        }

        public String getParticipantInn() {
            return participantInn;
        }

        public String getProducerInn() {
            return producerInn;
        }

        public String getProductionDate() {
            return productionDate;
        }

        public ProductionType getProductionType() {
            return productionType;
        }

        public String getRegNumber() {
            return regNumber;
        }

        public String getRegDate() {
            return regDate;
        }

        public void setProducts(List<Product> products) {
            this.products = products;
        }

        public List<Product> getProducts() {
            return products;
        }

        enum ProductionType {
            OWN_PRODUCTION,
            CONTRACT_PRODUCTION
        }

        static class Description {
            public String getParticipantInn() {
                return participantInn;
            }

            private final String participantInn;

            public Description(String participantInn) {
                this.participantInn = participantInn;
            }
        }

        static class Product {
            private CertificateType certificateDocument;
            private String certificateDocumentDate;
            private String certificateDocumentNumber;
            private final String productionDate;
            private final String ownerInn;
            private final String producerInn;
            private final String tnvedCode;
            private String uitCode;
            private String uituCode;

            public Product(String productionDate, String ownerInn, String producerInn, String tnvedCode) {
                this.productionDate = productionDate;
                this.ownerInn = ownerInn;
                this.producerInn = producerInn;
                this.tnvedCode = tnvedCode;
            }

            public void setCertificateDocument(CertificateType certificateDocument) {
                this.certificateDocument = certificateDocument;
            }

            public void setCertificateDocumentDate(String certificateDocumentDate) {
                this.certificateDocumentDate = certificateDocumentDate;
            }

            public void setCertificateDocumentNumber(String certificateDocumentNumber) {
                this.certificateDocumentNumber = certificateDocumentNumber;
            }

            public void setUitCode(String uitCode) {
                this.uitCode = uitCode;
            }

            public void setUituCode(String uituCode) {
                this.uituCode = uituCode;
            }

            public CertificateType getCertificateDocument() {
                return certificateDocument;
            }

            public String getCertificateDocumentDate() {
                return certificateDocumentDate;
            }

            public String getCertificateDocumentNumber() {
                return certificateDocumentNumber;
            }

            public String getProductionDate() {
                return productionDate;
            }

            public String getOwnerInn() {
                return ownerInn;
            }

            public String getProducerInn() {
                return producerInn;
            }

            public String getTnvedCode() {
                return tnvedCode;
            }

            public String getUitCode() {
                return uitCode;
            }

            public String getUituCode() {
                return uituCode;
            }

            public enum CertificateType {
                CONFORMITY_CERTIFICATE, CONFORMITY_DECLARATION
            }
        }
    }
}