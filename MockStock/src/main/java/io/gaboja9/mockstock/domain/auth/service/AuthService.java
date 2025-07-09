package io.gaboja9.mockstock.domain.auth.service;

import java.util.Map;
import java.util.Optional;

import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;

import io.gaboja9.mockstock.domain.auth.dto.MembersDetails;
import io.gaboja9.mockstock.domain.members.entity.Members;
import io.gaboja9.mockstock.domain.members.repository.MembersRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService extends DefaultOAuth2UserService {

	private final MembersRepository membersRepository;

	@Override
	public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {

		OAuth2User oAuth2User = super.loadUser(userRequest);
		log.info("oAuth2User = {}", oAuth2User);

		Map<String, Object> attributes = oAuth2User.getAttributes();

		String findname = attributes.get("name").toString();
		String findemail = attributes.get("email").toString();
		String findprofileImage = attributes.get("picture").toString();
		String registrationId = userRequest.getClientRegistration().getRegistrationId();

		Optional<Members> membersOptional = membersRepository.findByEmail(findemail);

		Members members = membersOptional.orElseGet(
			() -> {
				Members saved = Members.builder()
					.nickname(findname)
					.email(findemail)
					.provider(registrationId)
					.profileImage(findprofileImage)
					.cashBalance(30000000)
					.bankruptcyCnt(0)
					.build();
				return membersRepository.save(saved);
			}
		);

		return MembersDetails.builder()
			.memberId(members.getId())
			.name(members.getNickname())
			.role(members.getRole().name())
			.attributes(attributes)
			.build();
	}

}
