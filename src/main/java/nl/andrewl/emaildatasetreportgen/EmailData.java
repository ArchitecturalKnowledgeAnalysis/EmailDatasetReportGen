package nl.andrewl.emaildatasetreportgen;

import nl.andrewl.email_indexer.data.EmailEntry;
import nl.andrewl.email_indexer.data.EmailRepository;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.ZonedDateTime;
import java.util.*;

public final class EmailData {
	private final String messageId;
	private final String idHash;
	private final String subject;
	private final String sentFrom;
	private final ZonedDateTime date;
	private final List<String> tags;
	private final List<String> tagsViaReplies;
	private final boolean hidden;
	private final List<EmailData> replies;
	private final String body;

	public EmailData(
			String messageId,
			String subject,
			String sentFrom,
			ZonedDateTime date,
			List<String> tags,
			List<String> tagsViaReplies,
			boolean hidden,
			List<EmailData> replies,
			String body
	) {
		this.messageId = messageId;
		try {
			var digest = MessageDigest.getInstance("SHA-256");
			byte[] hash = digest.digest(messageId.getBytes(StandardCharsets.UTF_8));
			this.idHash = Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
		} catch (NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}

		this.subject = subject;
		this.sentFrom = sentFrom;
		this.date = date;
		this.tags = tags;
		this.tagsViaReplies = tagsViaReplies;
		this.hidden = hidden;
		this.replies = replies;
		this.body = body;
	}

	public static EmailData fromMessageId(String messageId, EmailRepository repo) {
		var optionalEmail = repo.findEmailById(messageId);
		if (optionalEmail.isEmpty()) return null;
		EmailEntry email = optionalEmail.get();
		EmailData data = new EmailData(
				email.messageId(),
				email.subject(),
				email.sentFrom(),
				email.date(),
				email.tags(),
				new ArrayList<>(),
				email.hidden(),
				new ArrayList<>(),
				email.body()
		);
		List<EmailData> replies = repo.findAllReplies(messageId).stream()
				.map(prev -> EmailData.fromMessageId(prev.messageId(), repo))
				.filter(Objects::nonNull)
				.toList();
		List<String> replyTags = replies.stream()
				.flatMap(emailData -> {
					Set<String> tags = new HashSet<>();
					tags.addAll(emailData.tags);
					tags.addAll(emailData.tagsViaReplies);
					return tags.stream();
				})
				.distinct()
				.sorted()
				.toList();
		data.replies.addAll(replies);
		data.tagsViaReplies.addAll(replyTags);
		return data;
	}

	public String messageId() {
		return messageId;
	}

	public String idHash() {
		return idHash;
	}

	public String subject() {
		return subject;
	}

	public String sentFrom() {
		return sentFrom;
	}

	public ZonedDateTime date() {
		return date;
	}

	public List<String> tags() {
		return tags;
	}

	public List<String> tagsViaReplies() {
		return tagsViaReplies;
	}

	public boolean hidden() {
		return hidden;
	}

	public List<EmailData> replies() {
		return replies;
	}

	public String body() {
		return body;
	}

	@Override
	public boolean equals(Object obj) {
		if (obj == this) return true;
		if (obj == null || obj.getClass() != this.getClass()) return false;
		var that = (EmailData) obj;
		return Objects.equals(this.messageId, that.messageId);
	}

	@Override
	public int hashCode() {
		return Objects.hash(messageId);
	}
}
