package com.zhike.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

/** 签发与解析 HS256 令牌。载荷里带 id / 姓名 / 用户类型 / 角色 / 权限点。 */
@Component
public class JwtTokenProvider {

    private final JwtProperties props;
    private final SecretKey key;

    public JwtTokenProvider(JwtProperties props) {
        this.props = props;
        this.key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(props.getSecret()));
    }

    public String issue(LoginUser user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getUsername())
                .claim("uid", user.getId())
                .claim("name", user.getRealName())
                .claim("utype", user.getUserType())
                .claim("roles", List.copyOf(user.getRoles()))
                .claim("perms", List.copyOf(user.getPerms()))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(props.getTtlHours() * 3600)))
                .signWith(key)
                .compact();
    }

    /** 解析失败（过期/签名不符/格式错误）一律返回 null，由过滤器决定是否放行。 */
    public LoginUser parse(String token) {
        try {
            Claims c = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload();
            @SuppressWarnings("unchecked")
            List<String> roles = c.get("roles", List.class);
            @SuppressWarnings("unchecked")
            List<String> perms = c.get("perms", List.class);
            Number uid = c.get("uid", Number.class);
            Number utype = c.get("utype", Number.class);
            return new LoginUser(
                    uid == null ? null : uid.longValue(),
                    c.getSubject(),
                    c.get("name", String.class),
                    utype == null ? 3 : utype.intValue(),
                    roles == null ? List.of() : roles,
                    perms == null ? List.of() : perms);
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long ttlSeconds() {
        return props.getTtlHours() * 3600;
    }
}
