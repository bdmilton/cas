package org.apereo.cas.gauth.credential;

import org.apereo.cas.authentication.OneTimeTokenAccount;
import org.apereo.cas.util.LoggingUtils;
import org.apereo.cas.util.crypto.CipherExecutor;

import com.warrenstrange.googleauth.IGoogleAuthenticator;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.data.mongodb.core.MongoOperations;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * This is {@link MongoDbGoogleAuthenticatorTokenCredentialRepository}.
 *
 * @author Misagh Moayyed
 * @since 5.0.0
 */
@Slf4j
@ToString
@Getter
public class MongoDbGoogleAuthenticatorTokenCredentialRepository extends BaseGoogleAuthenticatorTokenCredentialRepository {
    private final MongoOperations mongoTemplate;

    private final String collectionName;

    public MongoDbGoogleAuthenticatorTokenCredentialRepository(final IGoogleAuthenticator googleAuthenticator,
                                                               final MongoOperations mongoTemplate,
                                                               final String collectionName,
                                                               final CipherExecutor<String, String> tokenCredentialCipher) {
        super(tokenCredentialCipher, googleAuthenticator);
        this.mongoTemplate = mongoTemplate;
        this.collectionName = collectionName;
    }

    @Override
    public Collection<? extends OneTimeTokenAccount> get(final String username) {
        val query = new Query();
        query.addCriteria(Criteria.where("username").is(username));
        val r = this.mongoTemplate.find(query, GoogleAuthenticatorAccount.class, this.collectionName);
        return decode(r);
    }

    @Override
    public Collection<? extends OneTimeTokenAccount> load() {
        try {
            val r = this.mongoTemplate.findAll(GoogleAuthenticatorAccount.class, this.collectionName);
            return r.stream()
                .map(this::decode)
                .collect(Collectors.toList());

        } catch (final Exception e) {
            LoggingUtils.error(LOGGER, e);
        }
        return new ArrayList<>(0);
    }

    @Override
    public void save(final String userName, final String secretKey, final int validationCode, final List<Integer> scratchCodes) {
        val account = GoogleAuthenticatorAccount.builder()
            .username(userName)
            .secretKey(secretKey)
            .validationCode(validationCode)
            .scratchCodes(scratchCodes)
            .build();
        update(account);
    }

    @Override
    public OneTimeTokenAccount update(final OneTimeTokenAccount account) {
        val encodedAccount = encode(account);
        this.mongoTemplate.save(encodedAccount, this.collectionName);
        return encodedAccount;
    }

    @Override
    public void deleteAll() {
        this.mongoTemplate.remove(new Query(), GoogleAuthenticatorAccount.class, this.collectionName);
    }

    @Override
    public void delete(final String username) {
        val query = new Query();
        query.addCriteria(Criteria.where("username").is(username));
        this.mongoTemplate.remove(query, GoogleAuthenticatorAccount.class, this.collectionName);
    }

    @Override
    public long count() {
        val query = new Query();
        query.addCriteria(Criteria.where("username").exists(true));
        return this.mongoTemplate.count(query, GoogleAuthenticatorAccount.class, this.collectionName);
    }
}
