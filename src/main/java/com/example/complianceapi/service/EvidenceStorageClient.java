package com.example.complianceapi.service;

import com.example.complianceapi.model.Evidence;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class EvidenceStorageClient {

    private static final Logger logger = LoggerFactory.getLogger(EvidenceStorageClient.class);

    public void storeEvidence(List<Evidence> evidenceList) {
        // In a real implementation, this would use WebClient or RestTemplate
        // to send the evidence to another API endpoint.
        logger.info("Storing evidence (simulation):");
        evidenceList.forEach(evidence -> logger.info(" - {}", evidence));
    }
}
