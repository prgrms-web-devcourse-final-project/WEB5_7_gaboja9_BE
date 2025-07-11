package io.gaboja9.mockstock.domain.auth.service;

import java.util.Map;

import org.springframework.security.oauth2.core.user.OAuth2User;

import io.gaboja9.mockstock.domain.auth.dto.MembersDetails;

public class MembersDetailsFactory {
	public static MembersDetails membersDetails(String provider, OAuth2User oAuth2User) {

		Map<String, Object> attributes = oAuth2User.getAttributes();

		switch (provider.toUpperCase().trim()) {
			case "GOOGLE" -> {
				return MembersDetails.builder()
					.name(attributes.get("name").toString())
					.email(attributes.get("email").toString())
					.profileImage(attributes.get("picture").toString())
					.attributes(attributes)
					.build();
			}
			case "NAVER" -> {
				Map<String, String> properties = (Map<String, String>) attributes.get("response");
				return MembersDetails.builder()
					.name(properties.get("name"))
					.email(properties.get("id") + "@naver.com")
					.profileImage(properties.get("profile_image"))
					.attributes(attributes)
					.build();
			}
			/*
			properties={nickname=영민, profile_image=http://img1.kakaocdn.net/thumb/R640x640.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg, thumbnail_image=http://img1.kakaocdn.net/thumb/R110x110.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg}, kakao_account={profile_nickname_needs_agreement=false, profile_image_needs_agreement=false, profile={nickname=영민, thumbnail_image_url=http://img1.kakaocdn.net/thumb/R110x110.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg, profile_image_url=http://img1.kakaocdn.net/thumb/R640x640.q70/?fname=http://t1.kakaocdn.net/account_images/default_profile.jpeg, is_default_image=true, is_default_nickname=false}}}]
			 */
			case "KAKAO" -> {
				Map<String, String> properties = (Map<String, String>) attributes.get("properties");
				return MembersDetails.builder()
						.name(properties.get("nickname"))
						.email(attributes.get("id") + "@kakao.com")
						.profileImage(properties.get("profile_image"))
						.attributes(attributes)
						.build();
			}
			default -> throw new IllegalArgumentException("지원하지 않는 제공자: " + provider);
		}
	}
}
