 package io.gaboja9.mockstock.domain.auth.service;

 import io.gaboja9.mockstock.domain.members.enums.Role;
 import lombok.extern.slf4j.Slf4j;
 import org.junit.jupiter.api.BeforeEach;
 import org.junit.jupiter.api.DisplayName;
 import org.junit.jupiter.api.Test;
 import org.springframework.beans.factory.annotation.Autowired;
 import org.springframework.boot.test.context.SpringBootTest;

 import static org.junit.jupiter.api.Assertions.*;

 @Slf4j
 @SpringBootTest
 class JwtTokenProviderTests {

    @Autowired
    JwtTokenProvider provider;

    @Test
    @DisplayName("JWT 발급 테스트")
    void jwt_issue_test() throws Exception{

        Long targetId = 1L;
        Role targetRole = Role.MEMBER;

        String token = provider.issueAcceessToken(targetId, targetRole);
        log.info("token = {}", token);
    }
 }
