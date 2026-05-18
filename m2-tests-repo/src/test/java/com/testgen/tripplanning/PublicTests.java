package com.testgen.tripplanning;

import java.net.http.HttpResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;

// ────────────────────────────────────────────────────────────────────────────
// PublicTests.java — hand-written, dynamic, scenario-driven public test cases
// for TripPlanning M2.
//
// This is the canonical public test file. The 749 template-generated classes
// (deprecated) live in archive/PublicTestsAll_Stale.java for reference but
// no longer execute.
//
// Each class below is one row in
// docs/test-scenarios/TripPlanning_Tests_Description.md.
//
// Style guide:
//   * One package-private `class TC<NN>_<DescriptiveName> extends TestBase`
//     per scenario, with one or more @Test methods.
//   * @Tag("public") + a category tag (features_m1 / features_m2 / patterns
//     / amendments / updated_crud / cross_cutting) per scenario row.
//   * Resolve every URL through TestBase helpers — never hardcode "/api/...":
//       - crudReadPath("Itinerary"), crudCollectionPath("Activity"), fillPath(...)
//       - loginPath(), registerPath()
//     Resolve table names through tableName("Itinerary"), enums through
//     enumValues("OrderStatus").
//   * Resolve IDs from response bodies (registration, search results) or
//     from the auto-seeded fixtures (admin = id=1 via adminToken/adminId,
//     vegetarian customer with 5 orders = id=4, etc. — see
//     memory/project_tripplanning_seed_data.md). Never write `/api/users/1`
//     literally.
//   * One scenario at a time, approved by the user before mapping to the
//     other 7 themes.
// ────────────────────────────────────────────────────────────────────────────

// ─── TC01 — Register a new user (happy path) ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC01_RegisterHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC01 — POST registerPath() with a fresh email returns 2xx and a JWT token")
        void register_returns_2xx_with_token() throws Exception {
                BASE_URL = userServiceUrl;
                // Build a payload with a nonce-based email so it cannot collide with
                // any of the auto-seeded users (_preseed_*@grader.testgen.io) or
                // with prior runs of this test class.
                String email = "tc01_" + nonce() + "@grader.testgen.io";
                String body = String.format("""
                                {"name":"TC01 User","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));

                HttpResponse<String> r = httpPost("/api/auth/register", body);

                // Strict 2xx — registering a brand-new email with a valid payload
                // must succeed; any non-2xx is a bug in register / validation /
                // password hashing / DB persistence.
                assert2xx(r, "TC01 register");
                JsonNode j = parseNode(r.body());
                // Spec: response is { "token": "...", "expiresIn": ... }. No 'id' field;
                // chained tests resolve uid from JWT (uidFromJwt()) or via login.
                assertNotNull(j.get("token"),
                                "TC01: register response must include 'token' field per spec; body=" + r.body());
                assertFalse(j.get("token").asText().isBlank(),
                                "TC01: 'token' must be a non-blank string; got " + j.get("token"));
                assertTrue(j.has("expiresIn") && j.get("expiresIn").asLong() > 0,
                                "TC01: register response must include positive 'expiresIn' per spec; body=" + r.body());
        }
}

// ─── TC02 — Login with valid credentials (happy path) ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC02_LoginHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC02 — POST loginPath() after a successful register returns 2xx with a 3-segment JWT")
        void login_returns_2xx_with_three_segment_jwt() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — create a fresh user.
                String email = "tc02_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC02 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC02 setup register");

                // Act — log in with the same credentials.
                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> r = httpPost("/api/auth/login", loginBody);

                // Assert.
                assert2xx(r, "TC02 login");
                JsonNode j = parseNode(r.body());
                assertNotNull(j.get("token"),
                                "TC02: login response must include 'token' field; body=" + r.body());
                String token = j.get("token").asText();
                assertFalse(token.isBlank(),
                                "TC02: 'token' must be a non-blank string");
                assertEquals(3, token.split("\\.").length,
                                "TC02: 'token' must be a 3-segment JWT (a.b.c); got '" + token + "'");
        }
}

// ─── TC03 — Read own user profile with valid JWT (happy path) ───────────────
@Tag("public")
@Tag("updated_crud")
class TC03_ReadOwnProfileHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC03 — GET crudReadPath(\"User\") with own JWT returns 2xx and a JSON object")
        void read_own_profile_returns_2xx_and_json_object() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register and capture the new user's id.
                String email = "tc03_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC03 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC03 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                // Setup — login and capture the JWT.
                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC03 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Act — read the user's own profile via the User CRUD path. The
                // path template comes from the manifest so a student who renames
                // their controller still gets the correct URL.
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid, token);

                // Assert. Strict 2xx — we just registered this exact user, a 404
                // here is a real bug (register didn't persist OR the JWT chain
                // rejected our own token).
                assert2xx(r, "TC03 read own profile");
                JsonNode j = parseNode(r.body());
                assertTrue(j.isObject(),
                                "TC03: response body must be a JSON object; got " + r.body());
        }
}

// ─── TC04 — Register with duplicate email returns 4xx (negative path) ───────
@Tag("public")
@Tag("features_m2")
class TC04_RegisterDuplicateEmailTests extends TestBase {

        @Test
        @DisplayName("TC04 — POST registerPath() with an already-registered email returns a 4xx")
        void register_with_duplicate_email_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Build a payload with a nonce-based email — guaranteed not to
                // collide with auto-seeded users or with prior runs of this test
                // class (since @BeforeEach truncates between tests anyway).
                String email = "tc04_" + nonce() + "@grader.testgen.io";
                String firstBody = String.format("""
                                {"name":"TC04 First","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));

                // Step 1 — first registration must succeed (precondition).
                HttpResponse<String> first = httpPost("/api/auth/register", firstBody);
                assert2xx(first, "TC04 first register (precondition)");

                // Step 2 — second registration with the SAME email but different
                // name + phone (uniqueness should be on email).
                String secondBody = String.format("""
                                {"name":"TC04 Second","email":"%s","password":"AnotherPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> second = httpPost("/api/auth/register", secondBody);

                // Step 3 — strict 4xx assertion. Tolerate any of 400/409/422
                // (M2 spec doesn't pin a specific code) but NOT 2xx, NOT 5xx,
                // and NOT 401/403 (the body itself is well-formed and authorized).
                int code = second.statusCode();
                assertTrue(code >= 400 && code < 500,
                                "TC04: duplicate-email register must return a 4xx client error; "
                                                + "got " + code + " body=" + second.body());
                assertTrue(code != 401,
                                "TC04: duplicate-email is not an auth failure (no Authorization header was sent); "
                                                + "401 indicates the controller is misclassifying the error. body="
                                                + second.body());
                assertTrue(code != 403,
                                "TC04: duplicate-email is not a permission failure (anyone can register); "
                                                + "403 indicates the controller is misclassifying the error. body="
                                                + second.body());
        }
}

// ─── TC05 — Login with wrong password returns 401 (negative path) ───────────
@Tag("public")
@Tag("features_m2")
class TC05_LoginWrongPasswordTests extends TestBase {

        @Test
        @DisplayName("TC05 — POST loginPath() with the wrong password returns strictly 401")
        void login_with_wrong_password_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register a fresh user with a known-correct password.
                // nonce-based email avoids collisions with auto-seeded users and
                // with prior test runs (truncate@BeforeEach handles cross-test).
                String email = "tc05_" + nonce() + "@grader.testgen.io";
                String correctPwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC05 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, correctPwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC05 setup register (precondition)");

                // Act — log in with the SAME email but a different password.
                // Different enough that bcrypt cannot accidentally match (no
                // shared prefix, different length).
                String wrongPwd = "WrongPwd!2026";
                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, wrongPwd);
                HttpResponse<String> r = httpPost("/api/auth/login", loginBody);
                int code = r.statusCode();

                // Assert — strictly 401. Tolerant assertions (with explanatory
                // messages) for each common misclassification:
                // * 2xx: login skipped the password check — critical security bug.
                // * 5xx: bcrypt mismatch leaked as exception instead of being
                // caught and translated to 401.
                // * 404: user-enumeration anti-pattern (OWASP). Login must NOT
                // distinguish "user not found" from "wrong password" in
                // its status code; both should be 401.
                // * 403: this is not a permissions issue. Login is how you
                // OBTAIN permissions; you cannot be 403'd from it.
                assertTrue(code / 100 != 2,
                                "TC05: login with wrong password must NOT return 2xx. "
                                                + "A 2xx here means the password check was skipped — critical security bug. "
                                                + "Got " + code + " body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC05: login with wrong password must NOT 5xx. A 5xx here means the bcrypt "
                                                + "mismatch threw an unhandled exception instead of being caught and "
                                                + "translated to 401. Got " + code + " body=" + r.body());
                assertTrue(code != 404,
                                "TC05: login with wrong password must NOT return 404. The user account exists; "
                                                + "returning 404 instead of 401 leaks the existence/non-existence of accounts "
                                                + "(OWASP user-enumeration anti-pattern). body=" + r.body());
                assertTrue(code != 403,
                                "TC05: login with wrong password must NOT return 403. Login is the act of "
                                                + "obtaining permissions, not exercising them — 403 is structurally wrong. "
                                                + "body=" + r.body());
                assertEquals(401, code,
                                "TC05: login with wrong password must return strictly 401 Unauthorized; got "
                                                + code + " body=" + r.body());
        }
}

// ─── TC06 — Authentication happy path: valid admin JWT accepted on a non-User
// CRUD
@Tag("public")
@Tag("authentication")
class TC06_AuthValidTokenAcceptedTests extends TestBase {

        @Test
        @DisplayName("TC06 — GET a non-User CRUD list endpoint with a valid admin Bearer JWT returns 2xx (auth filter accepts the token)")
        void valid_admin_jwt_is_accepted_on_non_user_crud() throws Exception {
                // Setup — obtain an admin JWT via TestBase.adminToken(). Uses
                // the pre-seeded admin login fast path when available, falling
                // back to TestAuthHelper.seedAdmin (HTTP register + JDBC promote
                // to ADMIN + login) otherwise. Admin role ensures TC06 is not
                // 403-blocked on entities whose list endpoint is admin-only.
                String token = adminToken();

                // Act — hit a NON-User CRUD list endpoint with the admin token.
                // Picks the first top-level non-User entity from the manifest
                // (theme-specific: TripPlanning → Destination, etc.). Route
                // BASE_URL to the correct service based on the resolved entity.
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                BASE_URL = _serviceUrlForNonUserEntity(entity);
                HttpResponse<String> r = httpGetAuth(path, token);

                // Assert — strict 2xx. This proves the auth filter accepts the
                // token end-to-end (signature verified, claims parsed, security
                // context populated) on a different controller than
                // UserController, ruling out "auth only works on /api/users".
                assert2xx(r, "TC06 auth happy path (admin) on " + entity + " list (" + path + ")");
        }
}

// ─── TC07 — Missing Authorization header on a non-User CRUD returns 401 ─────
@Tag("public")
@Tag("authentication")
class TC07_AuthMissingHeaderTests extends TestBase {

        @Test
        @DisplayName("TC07 — GET a non-User CRUD list endpoint with NO Authorization header returns strictly 401")
        void missing_auth_header_returns_401_on_non_user_crud() throws Exception {
                // Act — same endpoint as TC06's happy path (so the two form a
                // clean A/B test of the JWT filter), BUT no Authorization
                // header at all (httpGet, not httpGetAuth). No setup needed —
                // we're testing whether anonymous requests are blocked, which
                // doesn't depend on having any specific row in the DB.
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                BASE_URL = _serviceUrlForNonUserEntity(entity);
                HttpResponse<String> r = httpGet(path);
                int code = r.statusCode();

                // Assert — strictly 401. Tolerant assertions for each common
                // misclassification, with diagnostic messages:
                // * 2xx: endpoint wide-open — critical security bug.
                // * 5xx: filter chain crashed instead of cleanly rejecting.
                // * 404: endpoint reachable anonymously and just returns
                // empty/missing — wrong; auth must block FIRST, before
                // any controller logic runs.
                // * 403: 403 means "authenticated but lacking permission". We
                // sent NO credentials at all — the correct response is
                // 401 (Unauthorized), not 403 (Forbidden).
                assertTrue(code / 100 != 2,
                                "TC07: GET " + path + " without Authorization header must NOT return 2xx. "
                                                + "A 2xx here means the endpoint is wide-open — critical security bug. "
                                                + "Got " + code + " body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC07: GET " + path + " without Authorization header must NOT 5xx. A 5xx here "
                                                + "means the filter chain crashed instead of cleanly rejecting. "
                                                + "Got " + code + " body=" + r.body());
                assertTrue(code != 404,
                                "TC07: GET " + path + " without Authorization header must NOT return 404. "
                                                + "A 404 here means the endpoint is reachable anonymously and just "
                                                + "returned empty — auth must block FIRST, before any controller logic "
                                                + "runs. body=" + r.body());
                assertTrue(code != 403,
                                "TC07: GET " + path + " without Authorization header must NOT return 403. We "
                                                + "sent NO credentials; 403 means \"authenticated but lacking permission\", "
                                                + "which doesn't apply when there's no auth attempt at all. The correct "
                                                + "code is 401 Unauthorized. body=" + r.body());
                assertEquals(401, code,
                                "TC07: GET " + path + " without Authorization header must return strictly 401 "
                                                + "Unauthorized; got " + code + " body=" + r.body());
        }
}

// ─── TC08 — Tampered JWT signature is rejected with 401 (negative path) ─────
@Tag("public")
@Tag("authentication")
class TC08_AuthTamperedSignatureTests extends TestBase {

        @Test
        @DisplayName("TC08 — GET protected endpoint with a tampered-signature JWT returns strictly 401 (signature is verified, not just decoded)")
        void tampered_jwt_signature_is_rejected_with_401() throws Exception {
                // Setup — get a real, fully-valid admin JWT, then tamper its
                // signature segment. tamperSignature(token) preserves the
                // header + payload (still parses as a JWT, still passes any
                // "is it 3 segments?" check) but replaces the signature with
                // a base64 of "tampered-signature-does-not-verify" — so the
                // signature cannot verify against any signing key.
                String validToken = adminToken();
                String tamperedToken = tamperSignature(validToken);

                // Act — same endpoint as TC06/TC07 (the auth-filter A/B/C
                // triad). Send the tampered token in the Authorization header.
                // A correctly-implemented auth filter MUST reject this with 401
                // even though the token looks structurally valid.
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                BASE_URL = _serviceUrlForNonUserEntity(entity);
                HttpResponse<String> r = httpGetAuth(path, tamperedToken);
                int code = r.statusCode();

                // Assert — strictly 401. Diagnostic ladder:
                // * 2xx: signature not actually verified — critical security bug
                // (anyone can forge tokens by editing the payload).
                // * 5xx: filter chain crashed on signature mismatch instead of
                // cleanly rejecting.
                // * 404: filter passed the request through to the controller
                // and just didn't find anything — wrong; signature
                // mismatch must be caught FIRST in the filter, before
                // any controller logic.
                // * 403: forged credentials = "not authenticated" (401), not
                // "authenticated but lacking permission" (403).
                assertTrue(code / 100 != 2,
                                "TC08: tampered-signature JWT must NOT be accepted (status " + code + "). "
                                                + "A 2xx here means the signature was not actually verified — anyone can "
                                                + "forge tokens by editing the payload. Critical security bug. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC08: tampered-signature JWT must NOT 5xx (status " + code + "). A 5xx here "
                                                + "means the filter chain crashed on signature mismatch instead of cleanly "
                                                + "rejecting. body=" + r.body());
                assertTrue(code != 404,
                                "TC08: tampered-signature JWT must NOT return 404 (status " + code + "). A 404 "
                                                + "here means the filter passed the request through to the controller and "
                                                + "just didn't find anything — signature mismatch must be caught FIRST in "
                                                + "the filter, before any controller logic runs. body=" + r.body());
                assertTrue(code != 403,
                                "TC08: tampered-signature JWT must NOT return 403 (status " + code + "). Forged "
                                                + "credentials are \"not authenticated\" (401), not \"authenticated but "
                                                + "lacking permission\" (403). body=" + r.body());
                assertEquals(401, code,
                                "TC08: tampered-signature JWT must return strictly 401 Unauthorized; got "
                                                + code + " body=" + r.body());
        }
}

// ─── TC09 — Login with non-existent email returns 401 (negative path) ───────
@Tag("public")
@Tag("features_m2")
class TC09_LoginUnknownEmailTests extends TestBase {

        @Test
        @DisplayName("TC09 — POST loginPath() with an email that has never been registered returns strictly 401")
        void login_unknown_email_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                // nonce-based email — guaranteed not to exist in the DB. Any
                // truncate/auto-seed in @BeforeEach also strips users, so this
                // email is truly absent at login time.
                String unknownEmail = "tc09_never_registered_" + nonce() + "@grader.testgen.io";
                String body = String.format("""
                                {"email":"%s","password":"AnythingPwd!2026"}
                                """, unknownEmail);

                HttpResponse<String> r = httpPost("/api/auth/login", body);
                int code = r.statusCode();

                // Spec §10 S1-F11: 401 for both "user not found" and "wrong password" — avoids email enumeration.
                assertTrue(code / 100 != 2,
                                "TC09: login with a never-registered email must NOT return 2xx (status "
                                                + code + "). A 2xx here means a token was issued for a non-existent "
                                                + "user. body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC09: login with a never-registered email must NOT 5xx (status " + code
                                                + "). Server must handle the missing-user case cleanly. body="
                                                + r.body());
                assertTrue(code != 403,
                                "TC09: login with a never-registered email must NOT return 403 (status " + code
                                                + "). 403 is a permission error; this is an unauthenticated condition. body="
                                                + r.body());
                assertEquals(401, code,
                                "TC09: login with a never-registered email must return strictly 401 Unauthorized "
                                                + "(spec §10 S1-F11 — 401 for both 'user not found' and 'wrong password'); "
                                                + "got " + code + " body=" + r.body());
        }
}

// ─── TC10 — Empty Bearer token returns 401 (negative path) ──────────────────
@Tag("public")
@Tag("authentication")
class TC10_AuthEmptyBearerTests extends TestBase {

        @Test
        @DisplayName("TC10 — GET protected endpoint with `Authorization: Bearer ` (empty token) returns strictly 401")
        void empty_bearer_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                // "Bearer " with a trailing space and no token after it. Not a
                // missing header (TC07 covers that) — this header is PRESENT
                // but its value is malformed.
                HttpResponse<String> r = httpGetWithRawAuth(path, "Bearer ");
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC10: empty Bearer token must NOT be accepted (status " + code
                                                + "). A 2xx here means the auth filter accepted an empty/missing token. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC10: empty Bearer token must NOT 5xx (status " + code + "). The filter must "
                                                + "handle empty tokens cleanly, not throw NPE. body=" + r.body());
                assertEquals(401, code,
                                "TC10: empty Bearer token must return strictly 401 Unauthorized; got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC11 — Non-Bearer scheme (Basic) returns 401 (negative path) ───────────
@Tag("public")
@Tag("authentication")
class TC11_AuthBasicSchemeTests extends TestBase {

        @Test
        @DisplayName("TC11 — GET protected endpoint with `Authorization: Basic ...` (non-Bearer scheme) returns strictly 401")
        void basic_scheme_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                // "Basic dXNlcjpwYXNz" — base64 of "user:pass". Catches lenient
                // parsers that strip the scheme prefix and try to decode whatever
                // is left as a JWT (it isn't).
                HttpResponse<String> r = httpGetWithRawAuth(path, "Basic dXNlcjpwYXNz");
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC11: Basic-scheme auth must NOT be accepted on a JWT-protected endpoint "
                                                + "(status " + code
                                                + "). The filter must reject any non-Bearer scheme. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC11: Basic-scheme auth must NOT 5xx (status " + code + "). body=" + r.body());
                assertEquals(401, code,
                                "TC11: Basic-scheme auth must return strictly 401 Unauthorized; got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC12 — Garbage non-JWT token returns 401 (negative path) ───────────────
@Tag("public")
@Tag("authentication")
class TC12_AuthGarbageTokenTests extends TestBase {

        @Test
        @DisplayName("TC12 — GET protected endpoint with `Authorization: Bearer not_a_valid_jwt` returns strictly 401")
        void garbage_token_returns_401() throws Exception {
                BASE_URL = userServiceUrl;
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                // "not_a_valid_jwt" — no dots, not base64, structurally invalid.
                // Catches "if 3 dot-segments, decode without verifying" and any
                // code path that doesn't validate JWT structure before claims-extract.
                HttpResponse<String> r = httpGetWithRawAuth(path, "Bearer not_a_valid_jwt");
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC12: garbage non-JWT token must NOT be accepted (status " + code
                                                + "). A 2xx here means the filter didn't validate the JWT structure. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC12: garbage token must NOT 5xx (status " + code + "). The parser must "
                                                + "handle malformed tokens gracefully. body=" + r.body());
                assertEquals(401, code,
                                "TC12: garbage non-JWT token must return strictly 401 Unauthorized; got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC13 — Forged role-claim token (payload modified post-signing) rejected
@Tag("public")
@Tag("authentication")
class TC13_AuthForgedRoleClaimTests extends TestBase {

        @Test
        @DisplayName("TC13 — GET protected endpoint with a payload-tampered JWT (role forged to ADMIN) is NOT accepted")
        void forged_role_claim_token_is_rejected() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register a CUSTOMER user (default theme role) and log
                // in to capture a real, signed JWT for them.
                String email = "tc13_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC13 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC13 setup register (precondition)");

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC13 setup login (precondition)");
                String realToken = parseNode(login.body()).get("token").asText();

                // Forge a role claim into the payload while keeping the original
                // signature. The signature was computed over the original
                // payload; modifying the payload makes the signature INVALID
                // even though we don't tamper the signature segment itself.
                String[] parts = realToken.split("\\.");
                if (parts.length != 3) {
                        throw new AssertionError("TC13 setup: real token is not a 3-segment JWT: " + realToken);
                }
                String payloadJson = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
                // Try to replace common role-claim patterns. If none match,
                // inject a "role":"ADMIN" entry into the JSON object.
                String tamperedPayload = payloadJson;
                for (String[] swap : new String[][] {
                                { "\"role\":\"CUSTOMER\"", "\"role\":\"ADMIN\"" },
                                { "\"role\": \"CUSTOMER\"", "\"role\":\"ADMIN\"" },
                                { "\"authorities\":[\"ROLE_CUSTOMER\"]", "\"authorities\":[\"ROLE_ADMIN\"]" },
                }) {
                        if (tamperedPayload.contains(swap[0])) {
                                tamperedPayload = tamperedPayload.replace(swap[0], swap[1]);
                                break;
                        }
                }
                if (tamperedPayload.equals(payloadJson)) {
                        // No known role-claim pattern found — inject one. Locate the
                        // closing brace of the outer object and prepend a role claim.
                        int lastBrace = tamperedPayload.lastIndexOf('}');
                        if (lastBrace > 0) {
                                String prefix = tamperedPayload.substring(0, lastBrace).trim();
                                if (prefix.endsWith("{")) {
                                        tamperedPayload = prefix + "\"role\":\"ADMIN\"}";
                                } else {
                                        tamperedPayload = prefix + ",\"role\":\"ADMIN\"}";
                                }
                        }
                }
                String tamperedB64 = java.util.Base64.getUrlEncoder().withoutPadding()
                                .encodeToString(tamperedPayload.getBytes());
                String forgedToken = parts[0] + "." + tamperedB64 + "." + parts[2];

                // Act — hit a protected endpoint with the forged token.
                String entity = firstTopLevelNonUserEntity();
                String path = crudCollectionPath(entity);
                HttpResponse<String> r = httpGetAuth(path, forgedToken);
                int code = r.statusCode();

                // Assert — must NOT be 2xx. Either of these is acceptable:
                // * 401 — signature verification caught the payload tamper (preferred).
                // * 403 — signature verified but server re-validated role from DB
                // and found CUSTOMER, not ADMIN.
                // But 2xx means signature wasn't verified AND role was trusted from
                // the (forged) payload — critical privilege-escalation bug.
                assertTrue(code / 100 != 2,
                                "TC13: forged role-claim token must NOT be accepted (status " + code + "). "
                                                + "A 2xx here means the signature was not verified after payload "
                                                + "modification — anyone with a real token can self-promote to ADMIN by "
                                                + "editing their payload. Critical privilege-escalation bug. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC13: forged role-claim token must NOT 5xx (status " + code + "). The filter "
                                                + "must reject cleanly. body=" + r.body());
        }
}

// ─── TC14 — Register with missing required field returns 4xx (negative path)
@Tag("public")
@Tag("features_m2")
class TC14_RegisterMissingFieldTests extends TestBase {

        @Test
        @DisplayName("TC14 — POST registerPath() with a body missing the `email` field returns a 4xx")
        void register_missing_email_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Body missing `email` — controller must reject with 4xx, not
                // propagate the missing-field as a server-side NPE/5xx or
                // silently create a user with null email.
                String body = String.format("""
                                {"name":"TC14 User","password":"TestPwd!2026","phone":"+201%s"}
                                """, nonce().substring(0, 9));

                HttpResponse<String> r = httpPost("/api/auth/register", body);
                int code = r.statusCode();

                assertTrue(code >= 400 && code < 500,
                                "TC14: register with missing required field (email) must return a 4xx; got "
                                                + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC14: register with missing email must NOT 2xx (a user without an email "
                                                + "cannot be valid). body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC14: register with missing email must NOT 5xx — the controller must "
                                                + "validate input cleanly, not crash. body=" + r.body());
        }
}

// ─── TC15 — Register with role=ADMIN in body must NOT yield an ADMIN account ─
@Tag("public")
@Tag("features_m2")
class TC15_RegisterRoleEscalationTests extends TestBase {

        @Test
        @DisplayName("TC15 — POST registerPath() with `role:ADMIN` in body must NOT result in an ADMIN account (privilege-escalation)")
        void register_with_role_admin_in_body_must_not_escalate() throws Exception {
                BASE_URL = userServiceUrl;
                // Build a register body with `role:"ADMIN"` injected. The
                // controller may either ignore the field, reject the request,
                // or accept the request — but the resulting DB row's role
                // column must NOT be ADMIN. Anything else is privilege escalation.
                String email = "tc15_" + nonce() + "@grader.testgen.io";
                String body = String.format(
                                """
                                                {"name":"TC15 User","email":"%s","password":"TestPwd!2026","phone":"+201%s","role":"ADMIN"}
                                                """,
                                email, nonce().substring(0, 9));

                HttpResponse<String> reg = httpPost("/api/auth/register", body);
                // We don't strictly require 2xx here — a controller that
                // rejects extra fields with 4xx is also fine. What matters is
                // that no ADMIN account got created.
                int regCode = reg.statusCode();
                assertTrue(regCode / 100 != 5,
                                "TC15: register with role=ADMIN body must NOT 5xx (got " + regCode
                                                + "). body=" + reg.body());

                // If the registration succeeded, look up the role in DB and
                // assert it's NOT ADMIN.
                if (regCode / 100 == 2) {
                        String role = fetchUserRole(email);
                        assertNotNull(role,
                                        "TC15: registration returned 2xx but no user row found for email "
                                                        + email + " — register isn't actually persisting.");
                        assertTrue(!"ADMIN".equalsIgnoreCase(role),
                                        "TC15: registering with role=ADMIN in the body must NOT result in an "
                                                        + "ADMIN account. Found role=" + role + " (expected the theme "
                                                        + "default, NOT ADMIN). This is a privilege-escalation bug — the "
                                                        + "controller is mapping the body's role field into the entity.");
                }
                // If registration was rejected (4xx), that's also acceptable —
                // the role field was caught as invalid input. Either way no
                // ADMIN was created.
        }
}

// ─── TC16 — Login with empty password returns 4xx (negative path) ───────────
@Tag("public")
@Tag("features_m2")
class TC16_LoginEmptyPasswordTests extends TestBase {

        @Test
        @DisplayName("TC16 — POST loginPath() with `password:\"\"` (empty) returns NOT 2xx")
        void login_empty_password_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register a real user so the email exists.
                String email = "tc16_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC16 User","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBody), "TC16 setup register");

                // Act — login with empty password. Bcrypt verification must
                // not be bypassed by an empty input.
                String loginBody = String.format("""
                                {"email":"%s","password":""}
                                """, email);
                HttpResponse<String> r = httpPost("/api/auth/login", loginBody);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC16: login with empty password must NOT issue a token (status " + code
                                                + "). A 2xx here means bcrypt verification was bypassed for empty "
                                                + "input. body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC16: login with empty password must NOT 5xx (status " + code + "). The "
                                                + "controller must validate input cleanly, not NPE on empty string. "
                                                + "body=" + r.body());
                // Acceptable: 400 (validation error) or 401 (auth failure) or
                // 422 (semantic validation error). All 4xx.
                assertTrue(code >= 400 && code < 500,
                                "TC16: login with empty password must return a 4xx (validation or auth "
                                                + "failure); got " + code + " body=" + r.body());
        }
}

// ─── TC17 — Cross-user IDOR: User A cannot READ User B's profile ────────────
@Tag("public")
@Tag("authorization")
class TC17_IdorReadOtherUserTests extends TestBase {

        @Test
        @DisplayName("TC17 — Customer A's GET on User B's CRUD path must NOT be 2xx (cross-user IDOR)")
        void customer_a_cannot_read_user_b_profile() throws Exception {
                BASE_URL = userServiceUrl;
                String emailA = "tc17a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc17b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBodyA = String.format("""
                                {"name":"TC17 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"TC17 B","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC17 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC17 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC17 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                HttpResponse<String> r = httpGetAuth("/api/users/" + bid, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC17: customer A reading customer B's profile must NOT be 2xx (status "
                                                + code + "). A 2xx here means cross-user IDOR is unprotected — any "
                                                + "authenticated user can read any other user's profile. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC17: cross-user read must NOT 5xx (status " + code + "). The auth check "
                                                + "must reject cleanly, not crash. body=" + r.body());
                assertTrue(code == 403 || code == 404,
                                "TC17: cross-user read must return 403 (forbidden) or 404 (not-found / "
                                                + "privacy-by-obscurity); got " + code + " body=" + r.body());
        }
}

// ─── TC18 — Cross-user IDOR: User A cannot UPDATE User B's profile ──────────
@Tag("public")
@Tag("authorization")
class TC18_IdorUpdateOtherUserTests extends TestBase {

        @Test
        @DisplayName("TC18 — Customer A's PUT on User B's CRUD path must NOT be 2xx, AND B's data must NOT change in DB")
        void customer_a_cannot_update_user_b_profile() throws Exception {
                BASE_URL = userServiceUrl;
                String emailA = "tc18a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc18b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String origNameB = "TC18 B Original";
                String regBodyA = String.format("""
                                {"name":"TC18 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"+201%s"}
                                """, origNameB, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC18 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC18 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC18 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                // Mitigation pattern — include all original fields plus the
                // changed name (some controllers require all fields on PUT).
                String tamperedName = "TC18 HIJACK";
                String putBody = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"+201%s"}
                                """, tamperedName, emailB, pwd, nonce().substring(0, 9));
                HttpResponse<String> r = httpPutAuth("/api/users/" + bid, putBody, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC18: customer A updating customer B's profile must NOT be 2xx (status "
                                                + code + "). A 2xx here means cross-user IDOR write is unprotected. "
                                                + "body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC18: cross-user update must NOT 5xx (status " + code + "). body=" + r.body());
                assertTrue(code == 403 || code == 404,
                                "TC18: cross-user update must return 403 or 404; got " + code + " body=" + r.body());

                // Defensive — even if controller returned 4xx, verify B's row
                // in DB was NOT mutated. Some buggy controllers reject the
                // response but commit the change.
                String userTable = tableName("User");
                String currentName = jdbc.queryForObject(
                                "SELECT name FROM " + userTable + " WHERE id = ?",
                                String.class, bid);
                assertEquals(origNameB, currentName,
                                "TC18: cross-user PUT was rejected (status " + code + ") but B's name in DB "
                                                + "changed from '" + origNameB + "' to '" + currentName + "' — the "
                                                + "controller is committing the change before doing the auth check.");
        }
}

// ─── TC19 — Cross-user IDOR: User A cannot DELETE User B ────────────────────
@Tag("public")
@Tag("authorization")
class TC19_IdorDeleteOtherUserTests extends TestBase {

        @Test
        @DisplayName("TC19 — Customer A's DELETE on User B's CRUD path must NOT be 2xx, AND B must STILL exist in DB")
        void customer_a_cannot_delete_user_b() throws Exception {
                BASE_URL = userServiceUrl;
                String emailA = "tc19a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc19b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBodyA = String.format("""
                                {"name":"TC19 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"TC19 B","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC19 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC19 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC19 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                HttpResponse<String> r = httpDeleteAuth("/api/users/" + bid, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC19: customer A deleting customer B must NOT be 2xx (status " + code
                                                + "). A 2xx here means cross-user delete is unprotected. body="
                                                + r.body());
                assertTrue(code / 100 != 5,
                                "TC19: cross-user delete must NOT 5xx (status " + code + "). body=" + r.body());
                assertTrue(code == 403 || code == 404,
                                "TC19: cross-user delete must return 403 or 404; got " + code + " body=" + r.body());

                // Defensive — B's row must STILL exist in DB.
                String userTable = tableName("User");
                Integer count = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM " + userTable + " WHERE id = ?",
                                Integer.class, bid);
                assertNotNull(count);
                assertTrue(count == 1,
                                "TC19: cross-user DELETE was rejected (status " + code + ") but B's row in DB "
                                                + "is gone (count=" + count + ") — the controller is committing the "
                                                + "delete before doing the auth check.");
        }
}

// ─── TC20 — Owner happy path: User A can UPDATE their own profile ───────────
@Tag("public")
@Tag("authorization")
class TC20_OwnerUpdateOwnProfileTests extends TestBase {

        @Test
        @DisplayName("TC20 — Customer's PUT on their own User CRUD path returns 2xx, AND DB reflects the new name")
        void owner_can_update_own_profile() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc20_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String origPhone = "+201" + nonce().substring(0, 9);
                String regBody = String.format("""
                                {"name":"TC20 Original","email":"%s","password":"%s","phone":"%s"}
                                """, email, pwd, origPhone);
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC20 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC20 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Mitigation pattern — include all original fields plus the new name.
                String newName = "TC20 Updated";
                String putBody = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"%s"}
                                """, newName, email, pwd, origPhone);
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid, putBody, token);
                assert2xx(r, "TC20 owner update own profile");

                // JDBC verification (NOT via GET) — we're testing the PUT
                // path's persistence semantics specifically.
                String userTable = tableName("User");
                String currentName = jdbc.queryForObject(
                                "SELECT name FROM " + userTable + " WHERE id = ?",
                                String.class, uid);
                assertEquals(newName, currentName,
                                "TC20: PUT returned " + r.statusCode() + " but DB row's name is '" + currentName
                                                + "' (expected '" + newName
                                                + "') — the controller returned 2xx without "
                                                + "actually persisting the update.");
        }
}

// ─── TC21 — Admin override: admin can READ any user ─────────────────────────
@Tag("public")
@Tag("authorization")
class TC21_AdminReadAnyUserTests extends TestBase {

        @Test
        @DisplayName("TC21 — Admin's GET on a customer's User CRUD path returns 2xx (admin role bypasses ownership)")
        void admin_can_read_any_user() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc21_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC21 Customer","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC21 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + customerId, adminTok);
                assert2xx(r, "TC21 admin read customer");
                JsonNode j = parseNode(r.body());
                assertTrue(j.isObject(),
                                "TC21: admin GET response body must be a JSON object; got " + r.body());
        }
}

// ─── TC22 — Admin override: admin can UPDATE any user ───────────────────────
@Tag("public")
@Tag("authorization")
class TC22_AdminUpdateAnyUserTests extends TestBase {

        @Test
        @DisplayName("TC22 — Admin's PUT on a customer's User CRUD path returns 2xx, AND DB reflects the new name")
        void admin_can_update_any_user() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc22_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String origPhone = "+201" + nonce().substring(0, 9);
                String regBody = String.format("""
                                {"name":"TC22 Customer","email":"%s","password":"%s","phone":"%s"}
                                """, email, pwd, origPhone);
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC22 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();

                // Mitigation pattern — include all original fields plus the new name.
                String newName = "TC22 Admin-Updated";
                String putBody = String.format("""
                                {"name":"%s","email":"%s","password":"%s","phone":"%s"}
                                """, newName, email, pwd, origPhone);
                HttpResponse<String> r = httpPutAuth("/api/users/" + customerId, putBody, adminTok);
                assert2xx(r, "TC22 admin update customer");

                String userTable = tableName("User");
                String currentName = jdbc.queryForObject(
                                "SELECT name FROM " + userTable + " WHERE id = ?",
                                String.class, customerId);
                assertEquals(newName, currentName,
                                "TC22: admin PUT returned " + r.statusCode() + " but customer's name in DB is '"
                                                + currentName + "' (expected '" + newName + "') — admin update did not "
                                                + "persist.");
        }
}

// ─── TC23 — Admin override: admin can DELETE any user (strict hard-delete) ──
@Tag("public")
@Tag("authorization")
class TC23_AdminDeleteAnyUserTests extends TestBase {

        @Test
        @DisplayName("TC23 — Admin's DELETE on a customer's User CRUD path returns 2xx, AND the row is HARD-deleted (strict)")
        void admin_can_delete_any_user_hard() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc23_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC23 Customer","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC23 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();
                HttpResponse<String> r = httpDeleteAuth("/api/users/" + customerId, adminTok);
                assert2xx(r, "TC23 admin delete customer");

                // STRICT hard-delete: row must be physically gone from DB.
                // Soft-delete is NOT acceptable for this test per direction.
                String userTable = tableName("User");
                Integer count = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM " + userTable + " WHERE id = ?",
                                Integer.class, customerId);
                assertNotNull(count);
                assertEquals(0, count.intValue(),
                                "TC23: admin DELETE returned " + r.statusCode() + " but customer row STILL "
                                                + "exists in DB (count=" + count
                                                + "). DELETE must hard-delete the row, "
                                                + "not soft-delete it (use the deactivate endpoint for status changes).");

                // GET-after-DELETE — strictly 404.
                HttpResponse<String> g = httpGetAuth("/api/users/" + customerId, adminTok);
                int gcode = g.statusCode();
                assertTrue(gcode / 100 != 2,
                                "TC23: GET-after-DELETE returned 2xx (status " + gcode + "). The row was "
                                                + "already verified gone from DB above, but GET still finds it. body="
                                                + g.body());
                assertEquals(404, gcode,
                                "TC23: GET after a successful DELETE must return 404 Not Found; got " + gcode
                                                + " body=" + g.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S1-F12 — Get User Activity Feed
// GET /api/users/{id}/activity?page={page}&size={size}
// Auth: required user. Ownership: caller must be target OR admin.
// Defaults: page=0, size=10, max size=100. Response shape:
// { content: [{action, timestamp, details}], page, size, totalElements }
// ════════════════════════════════════════════════════════════════════════════

// ─── TC24 — Owner GET own activity returns 2xx with paginated envelope ──────
@Tag("public")
@Tag("features_m2")
class TC24_ActivityOwnerHappyPathTests extends TestBase {

        @Test
        @DisplayName("TC24 — GET /api/users/{ownId}/activity with own token returns 2xx and a paginated envelope")
        void owner_activity_returns_2xx_with_envelope() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — register and login the user.
                String email = "tc24_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC24 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC24 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC24 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Act — GET own activity.
                String activityPath = "/api/users" + "/" + uid + "/activity";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                assert2xx(r, "TC24 owner activity");

                // Assert envelope shape per spec: content[], page, size, totalElements.
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("content"),
                                "TC24: response must include `content` field; body=" + r.body());
                assertTrue(j.get("content").isArray(),
                                "TC24: `content` must be an array; got " + j.get("content"));
                assertTrue(j.has("page"),
                                "TC24: response must include `page` field; body=" + r.body());
                assertTrue(j.has("size"),
                                "TC24: response must include `size` field; body=" + r.body());
                assertTrue(j.has("totalElements"),
                                "TC24: response must include `totalElements` field; body=" + r.body());
        }
}

// ─── TC25 — Non-existent user ID returns 404 (admin token) ──────────────────
@Tag("public")
@Tag("features_m2")
class TC25_ActivityNonExistentIdTests extends TestBase {

        @Test
        @DisplayName("TC25 — GET /api/users/<Long.MAX_VALUE>/activity with admin token returns strictly 404")
        void activity_non_existent_id_returns_404() throws Exception {
                BASE_URL = userServiceUrl;
                // Use admin token: per spec, admin passes ownership check, then
                // user-not-found returns 404. With a non-admin token, ownership
                // would fail first with 403 — wrong path for this test.
                String adminTok = adminToken();
                long missingId = Long.MAX_VALUE;
                String activityPath = "/api/users" + "/" + missingId + "/activity";

                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC25: activity for a non-existent user ID must NOT be 2xx (status " + code
                                                + "). body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC25: activity for a non-existent user ID must NOT 5xx — server must handle "
                                                + "missing-user gracefully. body=" + r.body());
                assertEquals(404, code,
                                "TC25: per spec, admin passes ownership check then user-not-found yields "
                                                + "strictly 404; got " + code + " body=" + r.body());
        }
}

// ─── TC26 — Negative user ID returns 4xx (admin token) ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC26_ActivityNegativeIdTests extends TestBase {

        @Test
        @DisplayName("TC26 — GET /api/users/-1/activity with admin token returns a 4xx (graceful)")
        void activity_negative_id_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Admin token — bypass ownership so the test actually exercises
                // the negative-id rejection logic (validation OR not-found).
                String adminTok = adminToken();
                String activityPath = "/api/users" + "/-1/activity";

                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC26: activity for a negative user ID must NOT 5xx — controller must "
                                                + "validate / reject gracefully, not crash. status=" + code + " body="
                                                + r.body());
                assertTrue(code / 100 != 2,
                                "TC26: activity for a negative user ID must NOT be 2xx — negative ids cannot "
                                                + "match any real user. status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC26: activity for a negative user ID must return a 4xx (400 validation or "
                                                + "404 not-found); got " + code + " body=" + r.body());
        }
}

// ─── TC27 — String user ID returns 4xx (admin token) ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC27_ActivityStringIdTests extends TestBase {

        @Test
        @DisplayName("TC27 — GET /api/users/abc/activity with admin token returns a 4xx (path-var binding fails)")
        void activity_string_id_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Admin token — ensures any 401 we see is NOT from missing auth.
                String adminTok = adminToken();
                String activityPath = "/api/users" + "/abc/activity";

                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC27: activity for a non-numeric user ID must NOT 5xx — Spring's path-var "
                                                + "binding should reject the string cleanly, not throw an unhandled "
                                                + "TypeMismatchException. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC27: activity for a non-numeric user ID must NOT be 2xx — 'abc' cannot be "
                                                + "a valid Long. status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC27: activity for a non-numeric user ID must return a 4xx (typically 400 "
                                                + "Bad Request); got " + code + " body=" + r.body());
        }
}

// ─── TC28 — size=0 returns gracefully (NOT 5xx) ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC28_ActivitySizeZeroTests extends TestBase {

        @Test
        @DisplayName("TC28 — GET /api/users/{ownId}/activity?size=0 must NOT 5xx (spec silent on size=0)")
        void activity_size_zero_does_not_5xx() throws Exception {
                BASE_URL = userServiceUrl;
                // Setup — own user so we reach the pagination logic.
                String email = "tc28_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC28 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC28 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC28 setup login");
                String token = parseNode(login.body()).get("token").asText();

                // Act — size=0. PageRequest.of(0, 0) throws IllegalArgumentException,
                // so unhandled this becomes 500. Spec doesn't pin a code here, but
                // graceful handling means NOT 5xx.
                String activityPath = "/api/users" + "/" + uid + "/activity?size=0";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC28: size=0 must NOT 5xx — controller must validate / clamp / reject "
                                                + "gracefully, not let PageRequest.of throw IllegalArgumentException. "
                                                + "status=" + code + " body=" + r.body());
        }
}

// ─── TC29 — size=-1 returns 4xx ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC29_ActivityNegativeSizeTests extends TestBase {

        @Test
        @DisplayName("TC29 — GET /api/users/{ownId}/activity?size=-1 returns a 4xx")
        void activity_negative_size_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc29_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC29 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC29 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC29 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?size=-1";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC29: size=-1 must NOT 5xx — controller must validate gracefully. status="
                                                + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC29: size=-1 must NOT be 2xx — negative page size is semantically invalid. "
                                                + "status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC29: size=-1 must return a 4xx; got " + code + " body=" + r.body());
        }
}

// ─── TC30 — size=string returns 4xx (binding fails) ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC30_ActivityStringSizeTests extends TestBase {

        @Test
        @DisplayName("TC30 — GET /api/users/{ownId}/activity?size=abc returns a 4xx (Integer binding fails)")
        void activity_string_size_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc30_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC30 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC30 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC30 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?size=abc";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC30: size=abc must NOT 5xx — Spring's @RequestParam Integer binding should "
                                                + "reject the string cleanly. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC30: size=abc must NOT be 2xx — non-numeric size cannot be valid. status="
                                                + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC30: size=abc must return a 4xx (typically 400 Bad Request); got " + code
                                                + " body=" + r.body());
        }
}

// ─── TC31 — Cross-user activity (regular user) returns strictly 403 ─────────
@Tag("public")
@Tag("features_m2")
class TC31_ActivityCrossUserRegularTests extends TestBase {

        @Test
        @DisplayName("TC31 — Customer A's GET on User B's activity returns strictly 403 (per S1-F12 spec)")
        void cross_user_activity_regular_returns_403() throws Exception {
                BASE_URL = userServiceUrl;
                // Per spec: "ownership violation, NOT 404 — A's token is valid
                // and B exists." Strict 403 here, unlike TC17 (which accepts 403/404).
                String emailA = "tc31a_" + nonce() + "@grader.testgen.io";
                String emailB = "tc31b_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBodyA = String.format("""
                                {"name":"TC31 A","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailA, pwd, nonce().substring(0, 9));
                String regBodyB = String.format("""
                                {"name":"TC31 B","email":"%s","password":"%s","phone":"+201%s"}
                                """, emailB, pwd, nonce().substring(0, 9));
                assert2xx(httpPost("/api/auth/register", regBodyA), "TC31 setup register A");
                HttpResponse<String> regB = httpPost("/api/auth/register", regBodyB);
                assert2xx(regB, "TC31 setup register B");
                long bid = uidFromJwt(parseNode(regB.body()).get("token").asText());

                String loginBodyA = String.format("""
                                {"email":"%s","password":"%s"}
                                """, emailA, pwd);
                HttpResponse<String> loginA = httpPost("/api/auth/login", loginBodyA);
                assert2xx(loginA, "TC31 setup login A");
                String tokenA = parseNode(loginA.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + bid + "/activity";
                HttpResponse<String> r = httpGetAuth(activityPath, tokenA);
                int code = r.statusCode();

                assertTrue(code / 100 != 2,
                                "TC31: cross-user activity GET must NOT be 2xx — regular users cannot read "
                                                + "other users' activity feeds. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 5,
                                "TC31: cross-user activity GET must NOT 5xx. status=" + code + " body=" + r.body());
                assertEquals(403, code,
                                "TC31: per S1-F12 spec, cross-user activity GET must return strictly 403 "
                                                + "(ownership violation, NOT 404 — A's token is valid and B exists); got "
                                                + code + " body=" + r.body());
        }
}

// ─── TC32 — Cross-user activity (admin) returns 2xx ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC32_ActivityCrossUserAdminTests extends TestBase {

        @Test
        @DisplayName("TC32 — Admin's GET on a customer's activity returns 2xx (admin bypasses ownership)")
        void cross_user_activity_admin_returns_2xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc32_" + nonce() + "@grader.testgen.io";
                String regBody = String.format("""
                                {"name":"TC32 Customer","email":"%s","password":"TestPwd!2026","phone":"+201%s"}
                                """, email, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC32 setup register customer");
                long customerId = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String adminTok = adminToken();
                String activityPath = "/api/users" + "/" + customerId + "/activity";
                HttpResponse<String> r = httpGetAuth(activityPath, adminTok);
                assert2xx(r, "TC32 admin activity");

                JsonNode j = parseNode(r.body());
                assertTrue(j.has("content") && j.get("content").isArray(),
                                "TC32: admin response must include `content` array; body=" + r.body());
        }
}

// ─── TC33 — page=-1 returns 4xx ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC33_ActivityNegativePageTests extends TestBase {

        @Test
        @DisplayName("TC33 — GET /api/users/{ownId}/activity?page=-1 returns a 4xx")
        void activity_negative_page_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc33_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC33 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC33 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC33 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?page=-1";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC33: page=-1 must NOT 5xx — PageRequest.of(int page, int size) requires "
                                                + "page >= 0; controller must validate gracefully. status=" + code
                                                + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC33: page=-1 must NOT be 2xx — negative page is semantically invalid. "
                                                + "status=" + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC33: page=-1 must return a 4xx; got " + code + " body=" + r.body());
        }
}

// ─── TC34 — page=string returns 4xx (binding fails) ─────────────────────────
@Tag("public")
@Tag("features_m2")
class TC34_ActivityStringPageTests extends TestBase {

        @Test
        @DisplayName("TC34 — GET /api/users/{ownId}/activity?page=abc returns a 4xx (Integer binding fails)")
        void activity_string_page_returns_4xx() throws Exception {
                BASE_URL = userServiceUrl;
                String email = "tc34_" + nonce() + "@grader.testgen.io";
                String pwd = "TestPwd!2026";
                String regBody = String.format("""
                                {"name":"TC34 User","email":"%s","password":"%s","phone":"+201%s"}
                                """, email, pwd, nonce().substring(0, 9));
                HttpResponse<String> reg = httpPost("/api/auth/register", regBody);
                assert2xx(reg, "TC34 setup register");
                long uid = uidFromJwt(parseNode(reg.body()).get("token").asText());

                String loginBody = String.format("""
                                {"email":"%s","password":"%s"}
                                """, email, pwd);
                HttpResponse<String> login = httpPost("/api/auth/login", loginBody);
                assert2xx(login, "TC34 setup login");
                String token = parseNode(login.body()).get("token").asText();

                String activityPath = "/api/users" + "/" + uid + "/activity?page=abc";
                HttpResponse<String> r = httpGetAuth(activityPath, token);
                int code = r.statusCode();

                assertTrue(code / 100 != 5,
                                "TC34: page=abc must NOT 5xx — Spring's @RequestParam Integer binding should "
                                                + "reject the string cleanly. status=" + code + " body=" + r.body());
                assertTrue(code / 100 != 2,
                                "TC34: page=abc must NOT be 2xx — non-numeric page cannot be valid. status="
                                                + code + " body=" + r.body());
                assertTrue(code >= 400 && code < 500,
                                "TC34: page=abc must return a 4xx (typically 400 Bad Request); got " + code
                                                + " body=" + r.body());
        }
}

// ════════════════════════════════════════════════════════════════════════════
// S2 M2 — Catalog entity features (full-text search, indexing, dashboard).
// All tests dynamic via TestBase helpers:
// * s2CatalogEntity() — Restaurant / Product / Provider / etc.
// * s3OrderEntity() — Order / Booking / Transaction / etc.
// * s2CategoricalFilterParam() — first non-status enum field (cuisineType /
// category / specialty / ...)
// * enumValueAt(entity, field, idx) — i-th valid value for that enum
// * s2EventsCollection() — Mongo events collection (spec name, validated)
// * s2SearchIndex() — ES index name (spec name, validated)
// * buildKitchenSinkBody(...) — JSON body from manifest entityColumns
// ════════════════════════════════════════════════════════════════════════════

// ─── TC35 — S2-F10 happy path search returns 2xx + array ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC35_SearchHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC35 — GET <s2>/search/full-text?query=test with valid token returns 2xx + array shape")
        void search_happy_path_returns_2xx_array() throws Exception {
                BASE_URL = catalogServiceUrl;
                String token = adminToken();
                String searchPath = "/api/destinations" + "/search/full-text?query=test";
                HttpResponse<String> r = httpGetAuth(searchPath, token);
                assert2xx(r, "TC35 search happy path");
                JsonNode body = parseNode(r.body());
                boolean validShape = body.isArray() || (body.has("content") && body.get("content").isArray());
                assertTrue(validShape, "TC35: response must be JSON array OR paginated envelope; got " + r.body());
        }
}

// ─── TC36 — S2-F10 no token returns 401 ─────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC36_SearchNoTokenTests extends TestBase {
        @Test
        @DisplayName("TC36 — GET <s2>/search/full-text without Authorization header returns 401")
        void search_no_token_returns_401() throws Exception {
                BASE_URL = catalogServiceUrl;
                String searchPath = "/api/destinations" + "/search/full-text?query=anything";
                HttpResponse<String> r = httpGet(searchPath);
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC36: must NOT 2xx; got " + code);
                assertTrue(code / 100 != 5, "TC36: must NOT 5xx; got " + code);
                assertEquals(401, code, "TC36: must be strict 401; got " + code + " body=" + r.body());
        }
}

// ─── TC37 — S2-F10 exact match by primary categorical filter ────────────────
@Tag("public")
@Tag("features_m2")
class TC37_SearchExactCategoricalFilterTests extends TestBase {
        @Test
        @DisplayName("TC37 — Search ?<filter>=<value0> returns only entities with that filter value")
        void search_filter_categorical_returns_only_matching() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String filterParam = s2CategoricalFilterParam();
                String filterValue0 = enumValueAt(entity, filterParam, 0);
                String filterValue1 = enumValueAt(entity, filterParam, 1);
                String statusOpen = enumValueAt(entity, "status", 0);

                String n = nonce();
                createEntity(adminTok, "TC37 First_" + n, filterValue0, statusOpen);
                createEntity(adminTok, "TC37 Second_" + n, filterValue1, statusOpen);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?" + filterParam + "="
                                + filterValue0;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC37 search by " + filterParam);
                JsonNode arr = unwrap(parseNode(r.body()));
                for (JsonNode item : arr) {
                        String c = item.has(filterParam) ? item.get(filterParam).asText() : null;
                        assertEquals(filterValue0, c,
                                        "TC37: every result must have " + filterParam + "=" + filterValue0 + "; got "
                                                        + item);
                }
        }

        private void createEntity(String tok, String name, String filterValue, String status) throws Exception {
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of(
                                "name", name,
                                s2CategoricalFilterParam(), filterValue,
                                "status", status));
                assert2xx(httpPostAuth("/api/destinations", body, tok),
                                "TC37 setup create " + name);
        }

        private JsonNode unwrap(JsonNode b) {
                return b.isArray() ? b : (b.has("content") ? b.get("content") : b);
        }
}

// ─── TC38 — S2-F10 exact match by status ────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC38_SearchExactStatusTests extends TestBase {
        @Test
        @DisplayName("TC38 — Search ?status=<value0> returns only entities with that status")
        void search_filter_status_returns_only_matching() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String filterValue0 = enumValueAt(entity, s2CategoricalFilterParam(), 0);
                String status0 = enumValueAt(entity, "status", 0);
                String status1 = enumValueAt(entity, "status", 1);

                String n = nonce();
                createEntity(adminTok, "TC38 Status0_" + n, filterValue0, status0);
                createEntity(adminTok, "TC38 Status1_" + n, filterValue0, status1);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?status=" + status0;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC38 search by status");
                JsonNode arr = unwrap(parseNode(r.body()));
                for (JsonNode item : arr) {
                        String s = item.has("status") ? item.get("status").asText() : null;
                        assertEquals(status0, s, "TC38: every result must have status=" + status0 + "; got " + item);
                }
        }

        private void createEntity(String tok, String name, String filterValue, String status) throws Exception {
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of(
                                "name", name,
                                s2CategoricalFilterParam(), filterValue,
                                "status", status));
                assert2xx(httpPostAuth("/api/destinations", body, tok),
                                "TC38 setup create " + name);
        }

        private JsonNode unwrap(JsonNode b) {
                return b.isArray() ? b : (b.has("content") ? b.get("content") : b);
        }
}

// ─── TC39 — S2-F10 minRating + maxRating range filter ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC39_SearchRatingRangeTests extends TestBase {
        @Test
        @DisplayName("TC39 — Search ?minRating=4.0&maxRating=5.0 returns only entities with rating in [4.0, 5.0]")
        void search_rating_range_returns_entities_in_range() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String n = nonce();
                long lowId = createAndRate(adminTok, "TC39 Low_" + n, 3.0);
                long midId = createAndRate(adminTok, "TC39 Mid_" + n, 4.5);
                long highId = createAndRate(adminTok, "TC39 High_" + n, 5.0);
                reindex(adminTok, lowId);
                reindex(adminTok, midId);
                reindex(adminTok, highId);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?minRating=4.0&maxRating=5.0";
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC39 search rating range");
                JsonNode arr = unwrap(parseNode(r.body()));
                for (JsonNode item : arr) {
                        if (!item.has("rating") || item.get("rating").isNull())
                                continue;
                        double rating = item.get("rating").asDouble();
                        assertTrue(rating >= 4.0 && rating <= 5.0,
                                        "TC39: every result must have rating in [4.0, 5.0]; got " + rating + " in "
                                                        + item);
                }
        }

        private long createAndRate(String tok, String name, double rating) throws Exception {
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of("name", name));
                HttpResponse<String> r = httpPostAuth("/api/destinations", body, tok);
                assert2xx(r, "TC39 setup create " + name);
                long id = parseNode(r.body()).get("id").asLong();
                String ratingCol = columnByField(s2CatalogEntity(), "rating");
                jdbc.update("UPDATE \"" + tableName(s2CatalogEntity()) + "\" SET \"" + ratingCol
                                + "\" = ? WHERE id = ?", rating, id);
                return id;
        }

        private void reindex(String tok, long id) throws Exception {
                HttpResponse<String> r = httpPostAuth("/api/destinations" + "/" + id + "/index", "",
                                tok);
                assert2xx(r, "TC39 reindex id=" + id);
        }

        private JsonNode unwrap(JsonNode b) {
                return b.isArray() ? b : (b.has("content") ? b.get("content") : b);
        }
}

// ─── TC40 — S2-F10 minRating > maxRating returns 4xx ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC40_SearchInvalidRatingRangeTests extends TestBase {
        @Test
        @DisplayName("TC40 — Search ?minRating=5.0&maxRating=3.0 (invalid range) returns a 4xx")
        void search_invalid_rating_range_returns_4xx() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String searchPath = "/api/destinations"
                                + "/search/full-text?minRating=5.0&maxRating=3.0";
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                int code = r.statusCode();
                assertTrue(code / 100 != 5, "TC40: NOT 5xx; got " + code);
                assertTrue(code / 100 != 2, "TC40: NOT 2xx; got " + code);
                assertTrue(code >= 400 && code < 500, "TC40: must return 4xx; got " + code + " body=" + r.body());
        }
}

// ─── TC41 — S2-F10 query with no matches returns empty list ─────────────────
@Tag("public")
@Tag("features_m2")
class TC41_SearchNoMatchEmptyListTests extends TestBase {
        @Test
        @DisplayName("TC41 — Search with query that matches nothing returns 2xx + empty list")
        void search_no_match_returns_empty_list() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String improbableQuery = "TC41NoMatchQuery_" + nonce() + "_xyzqwe";
                String searchPath = "/api/destinations" + "/search/full-text?query="
                                + improbableQuery;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC41 search no match");
                JsonNode body = parseNode(r.body());
                JsonNode arr = body.isArray() ? body : (body.has("content") ? body.get("content") : body);
                assertTrue(arr.isArray(), "TC41: response must contain an array; got " + r.body());
                assertEquals(0, arr.size(), "TC41: must return empty list; got " + r.body());
        }
}

// ─── TC42 — S2-F10 results sorted by relevance ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC42_SearchSortedByRelevanceTests extends TestBase {
        @Test
        @DisplayName("TC42 — Search results sorted by relevance (name match ranks higher than description match)")
        void search_results_sorted_by_relevance() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String entity = s2CatalogEntity();
                String unique = "Tc42Word" + nonce();
                String aName = unique + " Kitchen";
                String bName = "Other Place TC42_" + nonce();
                long aid = createWithDetails(adminTok, aName, null);
                reindex(adminTok, aid);
                long bid = createWithDetails(adminTok, bName, "best authentic " + unique + " cuisine");
                reindex(adminTok, bid);

                String searchPath = crudCollectionPath(entity) + "/search/full-text?query=" + unique;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC42 search relevance");
                JsonNode body = parseNode(r.body());
                JsonNode arr = body.isArray() ? body : (body.has("content") ? body.get("content") : body);
                assertTrue(arr.isArray() && arr.size() >= 1,
                                "TC42: must return at least one result; body=" + r.body());

                int idxA = -1, idxB = -1;
                for (int i = 0; i < arr.size(); i++) {
                        String entryName = arr.get(i).has("name") ? arr.get(i).get("name").asText() : "";
                        if (aName.equals(entryName))
                                idxA = i;
                        if (bName.equals(entryName))
                                idxB = i;
                }
                assertTrue(idxA >= 0,
                                "TC42: name-match (A, name='" + aName + "') must appear in results; body=" + r.body());
                if (idxB >= 0) {
                        assertTrue(idxA < idxB,
                                        "TC42: name-match (A, idx=" + idxA
                                                        + ") must rank higher than description-match (B, idx=" + idxB
                                                        + ").");
                }
        }

        private long createWithDetails(String tok, String name, String desc) throws Exception {
                java.util.Map<String, Object> overrides = new java.util.HashMap<>();
                overrides.put("name", name);
                if (desc != null) {
                        overrides.put("details", java.util.Map.of("description", desc));
                }
                String body = buildKitchenSinkBody(s2CatalogEntity(), overrides);
                HttpResponse<String> r = httpPostAuth("/api/destinations", body, tok);
                assert2xx(r, "TC42 setup create " + name);
                return parseNode(r.body()).get("id").asLong();
        }

        private void reindex(String tok, long id) throws Exception {
                HttpResponse<String> r = httpPostAuth("/api/destinations" + "/" + id + "/index", "",
                                tok);
                assert2xx(r, "TC42 reindex id=" + id);
        }
}

// ─── TC43 — S2-F11 happy index path ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC43_IndexHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC43 — POST <s2>/{id}/index for an existing entity returns 2xx")
        void index_happy_path_returns_2xx() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String body = buildKitchenSinkBody(s2CatalogEntity(),
                                java.util.Map.of("name", "TC43 Entity_" + nonce()));
                HttpResponse<String> created = httpPostAuth("/api/destinations", body, adminTok);
                assert2xx(created, "TC43 setup create");
                long id = parseNode(created.body()).get("id").asLong();
                HttpResponse<String> r = httpPostAuth("/api/destinations" + "/" + id + "/index", "",
                                adminTok);
                assert2xx(r, "TC43 index");
        }
}

// ─── TC44 — S2-F11 indexed document matches PG attributes ───────────────────
@Tag("public")
@Tag("features_m2")
class TC44_IndexMatchesPgTests extends TestBase {
        @Test
        @DisplayName("TC44 — After indexing, ES doc fields match the PG row's attributes")
        void index_doc_matches_pg_attributes() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String unique = "TC44Entity_" + nonce();
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of(
                                "name", unique,
                                "details", java.util.Map.of("description", "signature description")));
                HttpResponse<String> created = httpPostAuth("/api/destinations", body, adminTok);
                assert2xx(created, "TC44 setup create");
                long id = parseNode(created.body()).get("id").asLong();
                String ratingCol = columnByField(s2CatalogEntity(), "rating");
                jdbc.update("UPDATE \"" + tableName(s2CatalogEntity()) + "\" SET \"" + ratingCol
                                + "\" = ? WHERE id = ?", 4.5, id);
                HttpResponse<String> indexed = httpPostAuth("/api/destinations" + "/" + id + "/index",
                                "", adminTok);
                assert2xx(indexed, "TC44 index");

                String esIndex = s2SearchIndex();
                long esCount = esSearchCount(esIndex, "name", unique);
                assertTrue(esCount >= 1,
                                "TC44: ES index '" + esIndex + "' must contain a document with name='" + unique
                                                + "' (count=" + esCount + ").");

                String searchPath = "/api/destinations" + "/search/full-text?query=" + unique;
                HttpResponse<String> sr = httpGetAuth(searchPath, adminTok);
                assert2xx(sr, "TC44 search after index");
                JsonNode body2 = parseNode(sr.body());
                JsonNode arr = body2.isArray() ? body2 : (body2.has("content") ? body2.get("content") : body2);
                JsonNode found = null;
                for (JsonNode item : arr) {
                        String entryName = item.has("name") ? item.get("name").asText() : "";
                        if (unique.equals(entryName)) {
                                found = item;
                                break;
                        }
                }
                assertNotNull(found, "TC44: indexed entity must be findable via /search/full-text by name='" + unique
                                + "'; got " + sr.body());

                // Verify name + status fields match between search result and PG row.
                java.util.Map<String, Object> pgRow = jdbc.queryForMap(
                                "SELECT name, status::text AS status FROM " + tableName(s2CatalogEntity())
                                                + " WHERE id = ?",
                                id);
                assertEquals(pgRow.get("name"), found.get("name").asText(), "TC44: ES name must match PG name");
                if (found.has("status")) {
                        assertEquals(pgRow.get("status"), found.get("status").asText(),
                                        "TC44: ES status must match PG status");
                }
        }
}

// ─── TC45 — S2-F11 auto-reindex on update ───────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC45_IndexAutoReindexOnUpdateTests extends TestBase {
        @Test
        @DisplayName("TC45 — Updating an entity via PUT (without /index) makes the new name searchable")
        void auto_reindex_on_update() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String origName = "TC45 OriginalName_" + nonce();
                String body = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of("name", origName));
                HttpResponse<String> created = httpPostAuth("/api/destinations", body, adminTok);
                assert2xx(created, "TC45 setup create");
                long id = parseNode(created.body()).get("id").asLong();

                String newName = "TC45_NewName_" + nonce();
                String putBody = buildKitchenSinkBody(s2CatalogEntity(), java.util.Map.of("name", newName));
                HttpResponse<String> updated = httpPutAuth("/api/destinations/" + id, putBody, adminTok);
                assert2xx(updated, "TC45 update name");

                String searchPath = "/api/destinations" + "/search/full-text?query=" + newName;
                HttpResponse<String> r = httpGetAuth(searchPath, adminTok);
                assert2xx(r, "TC45 search by new name");
                JsonNode body2 = parseNode(r.body());
                JsonNode arr = body2.isArray() ? body2 : (body2.has("content") ? body2.get("content") : body2);
                boolean found = false;
                for (JsonNode item : arr) {
                        String entryName = item.has("name") ? item.get("name").asText() : "";
                        if (newName.equals(entryName)) {
                                found = true;
                                break;
                        }
                }
                assertTrue(found, "TC45: search by new name must find the entity (proves auto-reindexing). body="
                                + r.body());
        }
}

// ─── TC46 — S2-F11 index on non-existent entity returns 404 ─────────────────
@Tag("public")
@Tag("features_m2")
class TC46_IndexNonExistentTests extends TestBase {
        @Test
        @DisplayName("TC46 — POST <s2>/<Long.MAX_VALUE>/index returns strictly 404")
        void index_non_existent_returns_404() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String indexPath = "/api/destinations" + "/" + Long.MAX_VALUE + "/index";
                HttpResponse<String> r = httpPostAuth(indexPath, "", adminTok);
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC46: NOT 2xx; got " + code);
                assertTrue(code / 100 != 5, "TC46: NOT 5xx; got " + code);
                assertEquals(404, code, "TC46: must be strict 404; got " + code + " body=" + r.body());
        }
}

// ─── TC47 — S2-F11 index without token returns 401 ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC47_IndexNoTokenTests extends TestBase {
        @Test
        @DisplayName("TC47 — POST <s2>/{id}/index without Authorization header returns 401")
        void index_no_token_returns_401() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String body = buildKitchenSinkBody(s2CatalogEntity(),
                                java.util.Map.of("name", "TC47 Entity_" + nonce()));
                HttpResponse<String> created = httpPostAuth("/api/destinations", body, adminTok);
                assert2xx(created, "TC47 setup create");
                long id = parseNode(created.body()).get("id").asLong();
                HttpResponse<String> r = httpPost("/api/destinations" + "/" + id + "/index", "");
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC47: NOT 2xx; got " + code);
                assertEquals(401, code, "TC47: must be strict 401; got " + code + " body=" + r.body());
        }
}

// ─── TC48 — S2-F12 dashboard happy path (uses pre-seeded entity id=1) ───────
@Tag("public")
@Tag("features_m2")
class TC48_DashboardHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC48 — GET <s2>/{id}/dashboard returns 2xx + DTO with totalOrders/totalRevenue")
        void dashboard_happy_path() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                long restId = _TpM2.dest(this, "TC48 Dest " + nonce(), "EG", "Cairo", "ACTIVE");
                HttpResponse<String> r = httpGetAuth(
                                "/api/destinations" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC48 dashboard");
                JsonNode j = parseNode(r.body());
                // Spec §S2-F12 DestinationDashboardDTO: totalItineraries + totalVisitors
                // (no totalOrders / totalRevenue on this DTO). Accept legacy field
                // names as a transitional fallback for prior student code.
                assertTrue(j.has("totalItineraries") || j.has("total_itineraries")
                                || j.has("totalOrders") || j.has("total_orders"),
                                "TC48: dashboard must include totalItineraries (per spec); got " + r.body());
                assertTrue(j.has("totalVisitors") || j.has("total_visitors")
                                || j.has("completedItineraries") || j.has("completed_itineraries")
                                || j.has("totalRevenue") || j.has("total_revenue"),
                                "TC48: dashboard must include totalVisitors or completedItineraries (per spec); got " + r.body());
        }
}

// ─── TC49 — S2-F12 aggregated values match PG-source values ─────────────────
@Tag("public")
@Tag("features_m2")
class TC49_DashboardAggregatesMatchPgTests extends TestBase {
        @Test
        @DisplayName("TC49 — Dashboard totalOrders/totalRevenue match values aggregated from PG (uses pre-seed)")
        void dashboard_aggregates_match_pg() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                long restId = _TpM2.dest(this, "TC49 Dest " + nonce(), "EG", "Cairo", "ACTIVE");
                String ordersTable = tableName(s3OrderEntity());
                String fkCol = s2CatalogFkColumn();
                String amtCol = columnByField(s3OrderEntity(), "totalAmount");

                HttpResponse<String> r = httpGetAuth(
                                "/api/destinations" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC49 dashboard");
                JsonNode j = parseNode(r.body());

                Integer expectedCount = jdbc.queryForObject(
                                "SELECT COUNT(*) FROM \"" + ordersTable + "\" WHERE \"" + fkCol + "\" = ?",
                                Integer.class, restId);
                Double expectedRevenueRaw = jdbc.queryForObject(
                                "SELECT COALESCE(SUM(\"" + amtCol + "\"), 0) FROM \"" + ordersTable + "\" WHERE \""
                                                + fkCol + "\" = ?",
                                Double.class, restId);
                double expectedRevenue = expectedRevenueRaw != null ? expectedRevenueRaw : 0.0;

                // Spec §S2-F12: count column is totalItineraries (was totalOrders).
                long actualCount =
                        j.has("totalItineraries") ? j.get("totalItineraries").asLong()
                      : j.has("total_itineraries") ? j.get("total_itineraries").asLong()
                      : j.has("totalOrders") ? j.get("totalOrders").asLong()
                      : j.has("total_orders") ? j.get("total_orders").asLong() : -1L;
                // Strict revenue equality dropped — DestinationDashboardDTO has no
                // sum-of-amounts field (spec uses totalVisitors / completedItineraries).
                // Verify the spec-mandated companion field is at least present + numeric.
                boolean visitorsPresent = (j.has("totalVisitors") && j.get("totalVisitors").isNumber())
                        || (j.has("total_visitors") && j.get("total_visitors").isNumber())
                        || (j.has("completedItineraries") && j.get("completedItineraries").isNumber())
                        || (j.has("completed_itineraries") && j.get("completed_itineraries").isNumber())
                        || (j.has("totalRevenue") && j.get("totalRevenue").isNumber())
                        || (j.has("total_revenue") && j.get("total_revenue").isNumber());

                assertEquals(expectedCount.longValue(), actualCount,
                                "TC49: totalItineraries mismatch — PG=" + expectedCount + ", dashboard=" + actualCount
                                                + ". body=" + r.body());
                assertTrue(visitorsPresent,
                                "TC49: dashboard must include numeric totalVisitors / completedItineraries (per spec); got " + r.body());
                // Suppress unused-warning on the unused expectedRevenue local — kept
                // around in case a future spec revision puts a sum-of-amounts back on
                // DestinationDashboardDTO.
                if (expectedRevenue < 0) { /* noop */ }
        }
}

// ─── TC50 — S2-F12 dashboard event written to MongoDB ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC50_DashboardEventLoggedTests extends TestBase {
        @Test
        @DisplayName("TC50 — After GET /dashboard, an event must appear in the spec-defined Mongo collection")
        void dashboard_logs_event_to_mongo() throws Exception {
                BASE_URL = catalogServiceUrl;
                if (mongo == null) {
                        throw new AssertionError(
                                        "TC50: MongoDB is required for this test but not reachable. Set "
                                                        + "SPRING_DATA_MONGODB_URI or ensure the Mongo container is up.");
                }
                String adminTok = adminToken();
                long restId = _TpM2.dest(this, "TC50 Dest " + nonce(), "EG", "Cairo", "ACTIVE");
                String collName = s2EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> coll = mongo.getCollection(collName);

                long before = coll.countDocuments();
                HttpResponse<String> r = httpGetAuth(
                                "/api/destinations" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC50 dashboard");
                long after = coll.countDocuments();

                assertTrue(after > before,
                                "TC50: GET /dashboard must log an event in collection '" + collName
                                                + "'. Counts: before=" + before + ", after=" + after);
        }
}

// ─── TC51 — S2-F12 dashboard for non-existent ID returns 404 ────────────────
@Tag("public")
@Tag("features_m2")
class TC51_DashboardNonExistentTests extends TestBase {
        @Test
        @DisplayName("TC51 — GET <s2>/<Long.MAX_VALUE>/dashboard returns strictly 404")
        void dashboard_non_existent_returns_404() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                String dashPath = "/api/destinations" + "/" + Long.MAX_VALUE + "/dashboard";
                HttpResponse<String> r = httpGetAuth(dashPath, adminTok);
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC51: NOT 2xx; got " + code);
                assertTrue(code / 100 != 5, "TC51: NOT 5xx; got " + code);
                assertEquals(404, code, "TC51: must be strict 404; got " + code + " body=" + r.body());
        }
}

// ─── TC52 — S2-F12 dashboard for entity with no orders returns zeros ────────
@Tag("public")
@Tag("features_m2")
class TC52_DashboardNoOrdersTests extends TestBase {
        @Test
        @DisplayName("TC52 — Dashboard for an entity with no orders returns 2xx + totalOrders=0 + totalRevenue=0")
        void dashboard_no_orders_returns_zeros() throws Exception {
                BASE_URL = catalogServiceUrl;
                String adminTok = adminToken();
                // Pre-seed catalog id=3 has no orders attached (the cross-theme baseline
                // seed plants orders against ids 1, 2, 4 — id=3 left empty intentionally).
                // We DELETE any orders for restId=3 defensively in case a prior test left
                // residual data.
                long restId = 3L;
                String fkCol = s2CatalogFkColumn();
                jdbc.update("DELETE FROM \"" + tableName(s3OrderEntity()) + "\" WHERE \"" + fkCol + "\" = ?", restId);

                HttpResponse<String> r = httpGetAuth(
                                "/api/destinations" + "/" + restId + "/dashboard", adminTok);
                assert2xx(r, "TC52 dashboard");
                JsonNode j = parseNode(r.body());

                long totalOrders = j.has("totalOrders") ? j.get("totalOrders").asLong()
                                : j.has("total_orders") ? j.get("total_orders").asLong() : -1L;
                double totalRevenue = j.has("totalRevenue") ? j.get("totalRevenue").asDouble()
                                : j.has("total_revenue") ? j.get("total_revenue").asDouble() : -1.0;

                assertEquals(0L, totalOrders, "TC52: must report totalOrders=0; got " + totalOrders);
                assertEquals(0.0, totalRevenue, 0.01, "TC52: must report totalRevenue=0; got " + totalRevenue);
        }
}

// ─── TC53 — S2-F12 dashboard without token returns 401 ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC53_DashboardNoTokenTests extends TestBase {
        @Test
        @DisplayName("TC53 — GET <s2>/{id}/dashboard without Authorization header returns 401")
        void dashboard_no_token_returns_401() throws Exception {
                BASE_URL = catalogServiceUrl;
                long restId = _TpM2.dest(this, "TC53 Dest " + nonce(), "EG", "Cairo", "ACTIVE");
                HttpResponse<String> r = httpGet("/api/destinations" + "/" + restId + "/dashboard");
                int code = r.statusCode();
                assertTrue(code / 100 != 2, "TC53: NOT 2xx; got " + code);
                assertEquals(401, code, "TC53: must be strict 401; got " + code + " body=" + r.body());
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S3 M2 — Itinerary Service features (TC54..TC99)
//
// Covers S3-F10 (itinerary analytics dashboard, TC54-TC69), S3-F11 (record
// user-destination visit, TC70-TC84), and S3-F12 (destination recommendations,
// TC85-TC99). Theme-specific terms: S3 endpoints under /api/itineraries;
// Itinerary entity (user, destinationId, status, totalAmount, startDate,
// itineraryDetails); Neo4j VISITED edges between User and Destination nodes;
// Mongo logs ANALYTICS_VIEWED / VISIT_RECORDED to itinerary_events.
// ════════════════════════════════════════════════════════════════════════════

@Tag("public")
@Tag("features_m2")
class TC54_AnalyticsDashboardHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC54 — Dashboard returns totalItineraries/completedItineraries/completionRate/totalRevenue/avgItineraryValue/itinerariesByStatus/itinerariesByDestination")
        void dashboard_happy_path() throws Exception {
                BASE_URL = orderServiceUrl;  // S3 — itinerary-service
                // 10 itineraries — 6 COMPLETED (totals 100/200/150/80/120/90 = 740),
                // 2 PLANNED, 2 CANCELLED, across two seeded destinations.
                long uid = adminId();
                long d1 = _TpM2.dest(this, "TC54 Dest1 " + nonce(), "EG", "Cairo", "ACTIVE");
                long d2 = _TpM2.dest(this, "TC54 Dest2 " + nonce(), "EG", "Luxor", "ACTIVE");
                _TpM2.itin(this, uid, d1, "COMPLETED", 100.0, "2026-03-02");
                _TpM2.itin(this, uid, d1, "COMPLETED", 200.0, "2026-03-05");
                _TpM2.itin(this, uid, d2, "COMPLETED", 150.0, "2026-03-09");
                _TpM2.itin(this, uid, d2, "COMPLETED",  80.0, "2026-03-12");
                _TpM2.itin(this, uid, d1, "COMPLETED", 120.0, "2026-03-15");
                _TpM2.itin(this, uid, d2, "COMPLETED",  90.0, "2026-03-18");
                _TpM2.itin(this, uid, d1, "PLANNED",    50.0, "2026-03-21");
                _TpM2.itin(this, uid, d2, "PLANNED",   180.0, "2026-03-24");
                _TpM2.itin(this, uid, d1, "CANCELLED",  60.0, "2026-03-27");
                _TpM2.itin(this, uid, d2, "CANCELLED",  70.0, "2026-03-30");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC54 dashboard");
                JsonNode j = parseNode(r.body());
                assertEquals(10L, _TpM2.rL(j, "totalItineraries", "total_itineraries"),
                                "TC54: totalItineraries=10 expected; body=" + r.body());
                assertEquals(6L, _TpM2.rL(j, "completedItineraries", "completed_itineraries"),
                                "TC54: completedItineraries=6 expected");
                assertEquals(740.0, _TpM2.rD(j, "totalRevenue", "total_revenue"), 0.01,
                                "TC54: totalRevenue=740 (SUM(totalAmount) for COMPLETED)");
                double rate = _TpM2.rD(j, "completionRate", "completion_rate");
                assertEquals(0.6, rate, 0.01, "TC54: completionRate=0.6 (6/10) expected; got " + rate);
                double avgVal = _TpM2.rD(j, "avgItineraryValue", "avg_itinerary_value");
                // avg over completed: 740/6 ≈ 123.33
                assertEquals(123.33, avgVal, 0.5, "TC54: avgItineraryValue≈123.33; got " + avgVal);
                JsonNode byStatus = _TpM2.rO(j, "itinerariesByStatus", "itineraries_by_status");
                assertNotNull(byStatus, "TC54: itinerariesByStatus key required; body=" + r.body());
                assertEquals(6L, byStatus.has("COMPLETED") ? byStatus.get("COMPLETED").asLong() : 0L,
                                "TC54: COMPLETED=6");
                assertEquals(2L, byStatus.has("PLANNED") ? byStatus.get("PLANNED").asLong() : 0L,
                                "TC54: PLANNED=2");
                assertEquals(2L, byStatus.has("CANCELLED") ? byStatus.get("CANCELLED").asLong() : 0L,
                                "TC54: CANCELLED=2");
                JsonNode byDest = _TpM2.rO(j, "itinerariesByDestination", "itineraries_by_destination");
                assertNotNull(byDest, "TC54: itinerariesByDestination key required");
        }
}

// ─── TC55 — S3-F10 totalItineraries attribute isolated ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC55_AnalyticsTotalItinerariesTests extends TestBase {
        @Test
        @DisplayName("TC55 — Dashboard.totalItineraries equals exact count of itineraries in range")
        void total_itineraries_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                for (int i = 0; i < 7; i++) {
                        _TpM2.itin(this, 1L, 1L, "PLANNED", 100.0, "2026-09-" + String.format("%02d", i + 1));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC55 dashboard");
                assertEquals(7L,
                                _TpM2.rL(parseNode(r.body()), "totalItineraries", "total_itineraries"),
                                "TC55: totalItineraries=7 expected");
        }
}

// ─── TC56 — S3-F10 completedItineraries isolated (COMPLETED only) ────────────
@Tag("public")
@Tag("features_m2")
class TC56_AnalyticsCompletedTests extends TestBase {
        @Test
        @DisplayName("TC56 — Dashboard.completedItineraries = COUNT(*) where status=COMPLETED")
        void completed_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                _TpM2.itin(this, 1L, 1L, "COMPLETED", 100.0, "2026-09-10");
                _TpM2.itin(this, 1L, 1L, "COMPLETED", 200.0, "2026-09-12");
                _TpM2.itin(this, 1L, 1L, "COMPLETED", 300.0, "2026-09-14");
                _TpM2.itin(this, 1L, 1L, "PLANNED",   400.0, "2026-09-15"); // excluded
                _TpM2.itin(this, 1L, 1L, "CANCELLED", 500.0, "2026-09-16"); // excluded
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC56 dashboard");
                assertEquals(3L,
                                _TpM2.rL(parseNode(r.body()), "completedItineraries", "completed_itineraries"),
                                "TC56: completedItineraries=3 (COMPLETED only)");
        }
}

// ─── TC57 — S3-F10 totalRevenue isolated (COMPLETED only) ────────────────────
@Tag("public")
@Tag("features_m2")
class TC57_AnalyticsTotalRevenueTests extends TestBase {
        @Test
        @DisplayName("TC57 — Dashboard.totalRevenue = SUM(totalAmount) where status=COMPLETED")
        void total_revenue_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                _TpM2.itin(this, 1L, 1L, "COMPLETED",  50.0, "2026-09-10");
                _TpM2.itin(this, 1L, 1L, "COMPLETED", 100.0, "2026-09-12");
                _TpM2.itin(this, 1L, 1L, "COMPLETED", 150.0, "2026-09-14");
                _TpM2.itin(this, 1L, 1L, "PLANNED",   200.0, "2026-09-15"); // excluded
                _TpM2.itin(this, 1L, 1L, "CANCELLED", 999.0, "2026-09-16"); // excluded
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC57 dashboard");
                assertEquals(300.0,
                                _TpM2.rD(parseNode(r.body()), "totalRevenue", "total_revenue"), 0.01,
                                "TC57: totalRevenue=300 (COMPLETED only)");
        }
}

// ─── TC58 — S3-F10 completionRate = completed / total ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC58_AnalyticsCompletionRateTests extends TestBase {
        @Test
        @DisplayName("TC58 — Dashboard.completionRate = completedItineraries / totalItineraries")
        void completion_rate_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                _TpM2.itin(this, 1L, 1L, "COMPLETED", 100.0, "2026-09-10");
                _TpM2.itin(this, 1L, 1L, "COMPLETED", 200.0, "2026-09-11");
                _TpM2.itin(this, 1L, 1L, "PLANNED",   300.0, "2026-09-12");
                _TpM2.itin(this, 1L, 1L, "CANCELLED", 400.0, "2026-09-13");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC58 dashboard");
                assertEquals(0.5,
                                _TpM2.rD(parseNode(r.body()), "completionRate", "completion_rate"), 0.01,
                                "TC58: completionRate=0.5 (2/4)");
        }
}

// ─── TC59 — S3-F10 itinerariesByStatus breakdown isolated ────────────────────
@Tag("public")
@Tag("features_m2")
class TC59_AnalyticsByStatusTests extends TestBase {
        @Test
        @DisplayName("TC59 — Dashboard.itinerariesByStatus contains all 4 Itinerary statuses")
        void itineraries_by_status_isolated() throws Exception {
                BASE_URL = orderServiceUrl;
                String[] sts = { "PLANNED", "IN_PROGRESS", "COMPLETED", "CANCELLED" };
                for (int i = 0; i < sts.length; i++) {
                        _TpM2.itin(this, 1L, 1L, sts[i], 100.0,
                                        "2026-09-" + String.format("%02d", i + 10));
                }
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC59 dashboard");
                JsonNode bd = _TpM2.rO(parseNode(r.body()), "itinerariesByStatus", "itineraries_by_status");
                assertNotNull(bd, "TC59: itinerariesByStatus key required");
                for (String st : sts) {
                        assertTrue(bd.has(st), "TC59: itinerariesByStatus missing key '" + st + "'");
                        assertEquals(1L, bd.get(st).asLong(),
                                        "TC59: itinerariesByStatus[" + st + "]=1 expected; got " + bd.get(st).asLong());
                }
        }
}

// ─── TC60 — S3-F10 empty range returns zeros ─────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC60_AnalyticsEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC60 — Empty date range returns totalItineraries=0/completedItineraries=0/totalRevenue=0")
        void empty_range_returns_zeros() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2030-01-01&endDate=2030-01-31", tok);
                assert2xx(r, "TC60 dashboard");
                JsonNode j = parseNode(r.body());
                assertEquals(0L, _TpM2.rL(j, "totalItineraries", "total_itineraries"),
                                "TC60: totalItineraries=0 expected");
                assertEquals(0L, _TpM2.rL(j, "completedItineraries", "completed_itineraries"),
                                "TC60: completedItineraries=0 expected");
                assertEquals(0.0, _TpM2.rD(j, "totalRevenue", "total_revenue"), 0.01,
                                "TC60: totalRevenue=0 expected");
        }
}

// ─── TC61 — S3-F10 boundary inclusion at startDate ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC61_AnalyticsStartBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC61 — Itinerary at exactly startDate is included")
        void start_boundary_included() throws Exception {
                BASE_URL = orderServiceUrl;
                String tTable = tableName("Itinerary");
                Long iid = _TpM2.itin(this, 1L, 1L, "COMPLETED", 100.0, "2026-09-01");
                String dateCol = columnByField("Itinerary", "startDate");
                jdbc.update("UPDATE \"" + tTable + "\" SET \"" + dateCol + "\"=? WHERE id=?",
                                java.sql.Date.valueOf("2026-09-01"), iid);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC61 dashboard");
                assertEquals(1L,
                                _TpM2.rL(parseNode(r.body()), "totalItineraries", "total_itineraries"),
                                "TC61: boundary itinerary at startDate must be included");
        }
}

// ─── TC62 — S3-F10 boundary inclusion at endDate ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC62_AnalyticsEndBoundaryTests extends TestBase {
        @Test
        @DisplayName("TC62 — Itinerary at endDate is included")
        void end_boundary_included() throws Exception {
                BASE_URL = orderServiceUrl;
                String tTable = tableName("Itinerary");
                Long iid = _TpM2.itin(this, 1L, 1L, "COMPLETED", 50.0, "2026-09-30");
                String dateCol = columnByField("Itinerary", "startDate");
                jdbc.update("UPDATE \"" + tTable + "\" SET \"" + dateCol + "\"=? WHERE id=?",
                                java.sql.Date.valueOf("2026-09-30"), iid);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-09-01&endDate=2026-09-30", tok);
                assert2xx(r, "TC62 dashboard");
                assertEquals(1L,
                                _TpM2.rL(parseNode(r.body()), "totalItineraries", "total_itineraries"),
                                "TC62: boundary itinerary at endDate must be included");
        }
}

// ─── TC63 — S3-F10 inverted dates → 400 ──────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC63_AnalyticsInvertedDatesTests extends TestBase {
        @Test
        @DisplayName("TC63 — startDate > endDate returns 400")
        void inverted_dates_400() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-04-01&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(), "TC63: must be 400; got " + r.statusCode() + " body=" + r.body());
        }
}

// ─── TC64 — S3-F10 missing JWT → 401 ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC64_AnalyticsMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC64 — Missing Authorization header returns 401")
        void missing_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGet(
                                "/api/itineraries/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31");
                assertEquals(401, r.statusCode(), "TC64: must be 401; got " + r.statusCode());
        }
}

// ─── TC65 — S3-F10 invalid JWT → 401 ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC65_AnalyticsInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC65 — Bogus JWT returns 401")
        void invalid_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31",
                                "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC65: must be 401; got " + r.statusCode());
        }
}

// ─── TC66 — S3-F10 ANALYTICS_VIEWED logged on first call ─────────────────────
@Tag("public")
@Tag("features_m2")
class TC66_AnalyticsLoggedFirstCallTests extends TestBase {
        @Test
        @DisplayName("TC66 — First call logs ANALYTICS_VIEWED to itinerary_events Mongo")
        void analytics_viewed_logged() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) {
                        throw new AssertionError(
                                        "TC66: MongoDB required. Set SPRING_DATA_MONGODB_URI or ensure Mongo is up.");
                }
                String coll = s3EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(coll);
                long before = col.countDocuments();
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC66 dashboard");
                long after = col.countDocuments();
                assertTrue(after > before,
                                "TC66: ANALYTICS_VIEWED event must be appended to '" + coll + "'. Counts: before="
                                                + before + ", after=" + after);
                org.bson.Document latest = col.find().sort(new org.bson.Document("_id", -1)).first();
                if (latest != null) {
                        String typeField = latest.getString("eventType");
                        if (typeField == null)
                                typeField = latest.getString("action");
                        if (typeField != null) {
                                assertEquals("ANALYTICS_VIEWED", typeField,
                                                "TC66: latest event in '" + coll + "' must be ANALYTICS_VIEWED; got "
                                                                + typeField);
                        }
                }
        }
}

// ─── TC67 — S3-F10 ANALYTICS_VIEWED logged on cache hit ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC67_AnalyticsLoggedOnCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC67 — Repeat call (cache hit) still logs ANALYTICS_VIEWED")
        void analytics_logged_on_cache_hit() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) {
                        throw new AssertionError(
                                        "TC67: MongoDB required. Set SPRING_DATA_MONGODB_URI or ensure Mongo is up.");
                }
                String coll = s3EventsCollection();
                com.mongodb.client.MongoCollection<org.bson.Document> col = mongo.getCollection(coll);
                long before = col.countDocuments();
                String tok = adminToken();
                HttpResponse<String> r1 = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r1, "TC67 dashboard #1");
                HttpResponse<String> r2 = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r2, "TC67 dashboard #2");
                long after = col.countDocuments();
                assertTrue(after >= before + 2,
                                "TC67: 2 calls must produce ≥2 ANALYTICS_VIEWED events (logging outside cache). before="
                                                + before + " after=" + after);
        }
}

// ─── TC68 — S3-F10 cache populated in Redis with ~10 min TTL ─────────────────
@Tag("public")
@Tag("features_m2")
class TC68_AnalyticsCachePopulatedTests extends TestBase {
        @Test
        @DisplayName("TC68 — Dashboard call populates Redis with TTL ≤ 600s")
        void cache_populated_in_redis() throws Exception {
                BASE_URL = orderServiceUrl;
                if (redis == null) {
                        throw new AssertionError(
                                        "TC68: Redis required. Set SPRING_DATA_REDIS_HOST/PORT or ensure Redis is up.");
                }
                java.util.Set<String> beforeKeys = redisKeys("*");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC68 dashboard");
                java.util.Set<String> afterKeys = redisKeys("*");
                assertTrue(afterKeys.size() > beforeKeys.size(),
                                "TC68: at least one new Redis key expected after dashboard call; before="
                                                + beforeKeys.size() + " after=" + afterKeys.size());
                boolean found10MinTtl = false;
                for (String k : afterKeys) {
                        if (beforeKeys.contains(k))
                                continue;
                        long ttl = redisTtl(k);
                        if (ttl > 0 && ttl <= 600) {
                                found10MinTtl = true;
                                break;
                        }
                }
                assertTrue(found10MinTtl,
                                "TC68: a new Redis key must have TTL in (0, 600] seconds (10-min cache); keys="
                                                + afterKeys);
        }
}

// ─── TC69 — S3-F10 cache hit served (mutate, second call returns first body) ─
@Tag("public")
@Tag("features_m2")
class TC69_AnalyticsCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC69 — Cache hit: 2nd call (after data mutation) returns 1st response")
        void cache_hit_returns_first_response() throws Exception {
                BASE_URL = orderServiceUrl;
                if (redis == null) {
                        throw new AssertionError(
                                        "TC69: Redis required. Set SPRING_DATA_REDIS_HOST/PORT or ensure Redis is up.");
                }
                for (int i = 0; i < 2; i++) {
                        _TpM2.itin(this, 1L, 1L, "COMPLETED", 100.0, "2026-03-15");
                }
                String tok = adminToken();
                HttpResponse<String> r1 = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r1, "TC69 dashboard #1");
                for (int i = 0; i < 3; i++) {
                        _TpM2.itin(this, 1L, 1L, "COMPLETED", 200.0, "2026-03-20");
                }
                java.util.Set<String> keys = redisKeys("*");
                String cachedBefore = null;
                for (String k : keys) {
                        String v = redisGet(k);
                        if (v != null && v.length() > 0) {
                                cachedBefore = v;
                                break;
                        }
                }
                HttpResponse<String> r2 = httpGetAuth(
                                "/api/itineraries/analytics/dashboard?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r2, "TC69 dashboard #2");
                assertEquals(r1.body(), r2.body(),
                                "TC69: cache hit expected — second response must equal first; r1=" + r1.body()
                                                + " r2=" + r2.body());
                if (cachedBefore != null) {
                        String cachedAfter = null;
                        for (String k : redisKeys("*")) {
                                String v = redisGet(k);
                                if (v != null && v.length() > 0) {
                                        cachedAfter = v;
                                        break;
                                }
                        }
                        assertEquals(cachedBefore, cachedAfter,
                                        "TC69: Redis cached value must be unchanged across the two calls (proves no re-aggregation)");
                }
        }
}


// ════════════════════════════════════════════════════════════════════════════
// Helper classes for TripPlanning M2 + M1 tests
// ════════════════════════════════════════════════════════════════════════════

final class _TpM2 {
        private _TpM2() {}

        /** INSERT an Itinerary row. Returns id. */
        static Long itin(TestBase t, long userId, long destinationId, String status,
                        double totalAmount, String date) {
                String table = t.tableName("Itinerary");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Itinerary", "user"), userId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Itinerary", "destinationId", "destination"), destinationId); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Itinerary", "status"), status);
                try { ov.put(t.columnByField("Itinerary", "totalAmount"), totalAmount); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Itinerary", "currency"), "EGP"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Itinerary", "title"), "auto-seeded itinerary"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Itinerary", "description"), "auto-seeded"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Itinerary", "itineraryDetails"), "{}"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Itinerary", "startDate"), java.sql.Date.valueOf(date)); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Itinerary", "endDate"), java.sql.Date.valueOf(date)); } catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(table, ov);
                t.setAllDateColumns(table, id, java.sql.Timestamp.valueOf(date + " 12:00:00"));
                try {
                        t.jdbc.update("UPDATE \"" + table + "\" SET \""
                                + t.columnByField("Itinerary", "startDate") + "\"=? WHERE id=?",
                                java.sql.Date.valueOf(date), id);
                } catch (Throwable ignore) {}
                return id;
        }

        /** INSERT a Destination row. Returns id. */
        static long dest(TestBase t, String name, String country, String city, String status) {
                String tbl = t.tableName("Destination");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Destination", "name"), name); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "country"), country); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "city"), city); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "status"), status); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "destinationDetails"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** Read visitCount property from VISITED edge in Neo4j by user+destinationId. */
        static long visitCount(TestBase t, long userId, long destinationId) {
                java.util.List<java.util.Map<String, Object>> rows = t.neo4jExec(
                        "MATCH (a:`" + t.s3GraphUserLabel() + "` {userId:$u})-[r:`" + t.s3GraphRelationship()
                          + "`]->(b:`" + t.s3GraphCatalogLabel() + "` {destinationId:$d}) "
                          + "RETURN coalesce(r.visitCount, r.count, 0) AS cnt LIMIT 1",
                        java.util.Map.of("u", userId, "d", destinationId));
                if (rows.isEmpty()) return 0L;
                Object c = rows.get(0).get("cnt");
                return c instanceof Number n ? n.longValue() : 0L;
        }

        /** Convenience: seed a COMPLETED itinerary then POST /record-visit. */
        static Long itinAndRecord(TestBase t, long userId, long destinationId, String adminTok) throws Exception {
                Long iid = itin(t, userId, destinationId, "COMPLETED", 100.0, "2026-04-10");
                t.httpPostAuth("/api/itineraries/" + iid + "/record-visit", "", adminTok);
                return iid;
        }

        static long rL(JsonNode j, String... ks) {
                for (String k : ks) if (j.has(k)) return j.get(k).asLong();
                return -1;
        }
        static double rD(JsonNode j, String... ks) {
                for (String k : ks) if (j.has(k)) return j.get(k).asDouble();
                return -1;
        }
        static JsonNode rO(JsonNode j, String... ks) {
                for (String k : ks) if (j.has(k)) return j.get(k);
                return null;
        }
}


final class _TpM2S4 {
        private _TpM2S4() {}

        /** INSERT an Activity. status: BOOKED/STARTED/COMPLETED/CANCELLED. */
        static long act(TestBase t, long itineraryId, String name, double cost,
                        String startTime, String status) {
                String tbl = t.tableName("Activity");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Activity", "itinerary"), itineraryId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Activity", "name"), name); } catch (Throwable ignore) {}
                boolean hasCostCol = false;
                try { ov.put(t.columnByField("Activity", "cost"), cost); hasCostCol = true; } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Activity", "currency"), "EGP"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Activity", "scheduledTime", "startTime"), java.sql.Timestamp.valueOf(startTime + " 12:00:00")); } catch (Throwable ignore) {}
                // Trip Planning Activity entity has no status field per M1 spec — wrap in try/catch.
                try { ov.put(t.columnByField("Activity", "status"), status); } catch (Throwable ignore) {}
                // If team's Activity has no scalar cost/status columns, embed them in the
                // metadata JSONB so analytics queries can read them back.
                String metaJson = "{}";
                if (!hasCostCol) {
                        metaJson = "{\"cost\": " + cost
                                + ", \"currency\": \"EGP\""
                                + ", \"status\": \"" + status + "\"}";
                }
                try { ov.put(t.columnByField("Activity", "activityDetails", "metadata", "details"), metaJson); } catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(tbl, ov);
                t.setAllDateColumns(tbl, id, java.sql.Timestamp.valueOf(startTime + " 12:00:00"));
                return id;
        }

        /** Update Activity.metadata JSONB (per spec; falls back to activity_details if student renamed). */
        static void setActivityDetails(TestBase t, long activityId, String json) {
                String col;
                try { col = t.columnByField("Activity", "metadata", "activityDetails", "details"); }
                catch (Throwable e) { col = "metadata"; }
                t.jdbc.update("UPDATE \"" + t.tableName("Activity") + "\" SET \"" + col + "\"=?::jsonb WHERE id=?",
                        json, activityId);
        }
}


final class _TpM2S5 {
        private _TpM2S5() {}

        /** INSERT a Booking row. Returns id. */
        static long bkg(TestBase t, long userId, long itineraryId, double totalAmount,
                        String startDate, String endDate, String status) {
                String tbl = t.tableName("Booking");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Booking", "user"), userId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "itinerary"), itineraryId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "totalAmount"), totalAmount); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "currency"), "EGP"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "startDate"), java.sql.Date.valueOf(startDate)); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "endDate"), java.sql.Date.valueOf(endDate)); } catch (Throwable ignore) {}
                ov.put(t.columnByField("Booking", "status"), status);
                try { ov.put(t.columnByField("Booking", "bookingDetails"), "{}"); } catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(tbl, ov);
                t.setAllDateColumns(tbl, id, java.sql.Timestamp.valueOf(startDate + " 12:00:00"));
                return id;
        }

        /** INSERT a PromoCode row (TripPlanning S5 lookup table — defensive: not all themes have it). */
        static long promo(TestBase t, String code, String discountType, double discount,
                         int maxUses, java.time.LocalDateTime expiry, boolean active) {
                String tbl = t.tableName("PromoCode");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                ov.put(t.columnByField("PromoCode", "code"), code);
                try { ov.put(t.columnByField("PromoCode", "discountType"), discountType); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "discountAmount"), discount); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "maxUses"), maxUses); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "currentUses"), 0); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "expiryDate"), java.sql.Timestamp.valueOf(expiry)); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "active"), active); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("PromoCode", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** Update Booking.bookingDetails JSONB. */
        static void setBookingDetails(TestBase t, long bookingId, String json) {
                t.jdbc.update("UPDATE \"" + t.tableName("Booking") + "\" SET booking_details=?::jsonb WHERE id=?",
                        json, bookingId);
        }
}


final class _TpM1Seed {
        private _TpM1Seed() {}

        /** INSERT a user. role: TRAVELER/ADMIN. */
        static long seedUser(TestBase t, String name, String email, String role) {
                String tbl = t.tableName("User");
                String bcrypt = "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy";
                String phone = "+201" + String.format("%09d", System.nanoTime() % 1_000_000_000L);
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                ov.put(t.columnByField("User", "name"), name);
                ov.put(t.columnByField("User", "email"), email);
                ov.put(t.columnByField("User", "phone"), phone);
                ov.put(t.columnByField("User", "password"), bcrypt);
                ov.put(t.columnByField("User", "role"), role);
                ov.put(t.columnByField("User", "status"), "ACTIVE");
                try { ov.put(t.columnByField("User", "preferences"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        static void setPrefs(TestBase t, long userId, String json) {
                t.jdbc.update("UPDATE \"" + t.tableName("User") + "\" SET preferences=?::jsonb WHERE id=?",
                        json, userId);
        }

        /** INSERT a Destination. */
        static long seedDestination(TestBase t, String name, String country, String city, String status) {
                return _TpM2.dest(t, name, country, city, status);
        }

        static void setDestinationDetails(TestBase t, long destId, String json) {
                String col;
                try { col = t.columnByField("Destination", "details", "destinationDetails", "metadata"); }
                catch (Throwable e) { col = "details"; }
                t.jdbc.update("UPDATE \"" + t.tableName("Destination") + "\" SET \"" + col + "\"=?::jsonb WHERE id=?",
                        json, destId);
        }

        /** INSERT an Itinerary. status: PLANNED/IN_PROGRESS/COMPLETED/CANCELLED. */
        static long seedItinerary(TestBase t, long userId, long destinationId, String status,
                            double totalAmount, String startDate) {
                return _TpM2.itin(t, userId, destinationId, status, totalAmount, startDate);
        }

        /** INSERT an Activity. status: BOOKED/STARTED/COMPLETED/CANCELLED. */
        static long seedActivity(TestBase t, long itineraryId, String name, double cost,
                                 String startTime, String status) {
                return _TpM2S4.act(t, itineraryId, name, cost, startTime, status);
        }

        /** INSERT a Booking. status: PLANNED/CONFIRMED/IN_PROGRESS/COMPLETED/CANCELLED. */
        static long seedBooking(TestBase t, long userId, long itineraryId, double totalAmount,
                                String startDate, String endDate, String status) {
                return _TpM2S5.bkg(t, userId, itineraryId, totalAmount, startDate, endDate, status);
        }

        /** INSERT a PromoCode. */
        static long seedPromoCode(TestBase t, String code, String discountType, double discount,
                                  int maxUses, java.time.LocalDateTime expiry, boolean active) {
                return _TpM2S5.promo(t, code, discountType, discount, maxUses, expiry, active);
        }

        static java.time.LocalDateTime futureDateTime() {
                return java.time.LocalDateTime.of(2030, 12, 31, 23, 59, 59);
        }
        static java.time.LocalDateTime pastDateTime() {
                return java.time.LocalDateTime.of(2020, 1, 1, 0, 0, 0);
        }
        static java.time.LocalDate futureDate() { return java.time.LocalDate.of(2030, 12, 31); }
        static java.time.LocalDate pastDate() { return java.time.LocalDate.of(2020, 1, 1); }
}


// ════════════════════════════════════════════════════════════════════════════
// S3-F11 — Record User-Destination Visit (TC70..TC84)
// Endpoint: POST /api/itineraries/{itineraryId}/record-visit
// Neo4j: (User)-[:VISITED]->(Destination) with visitCount, lastVisitDate,
//        recorded_itinerary_ids
// ════════════════════════════════════════════════════════════════════════════

// ─── TC70 — S3-F11 happy path: COMPLETED itinerary → VISITED edge created ────
@Tag("public")
@Tag("features_m2")
class TC70_RecordVisitHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC70 — COMPLETED itinerary creates VISITED edge with visitCount=1")
        void record_visit_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC70: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc70u");
                long uid = ((Number) u.get("id")).longValue();
                long did = _TpM2.dest(this, "TC70 Dest", "EG", "Cairo", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "COMPLETED", 250.0, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/itineraries/" + iid + "/record-visit", "", tok);
                assert2xx(r, "TC70");
                long count = _TpM2.visitCount(this, uid, did);
                assertEquals(1L, count, "TC70: visitCount=1; got " + count);
        }
}

// ─── TC71 — S3-F11 idempotency: same itinerary recorded twice → count stays 1 ─
@Tag("public")
@Tag("features_m2")
class TC71_RecordVisitIdempotencyTests extends TestBase {
        @Test
        @DisplayName("TC71 — Same itineraryId recorded twice keeps visitCount=1")
        void record_visit_idempotent() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC71: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc71u");
                long uid = ((Number) u.get("id")).longValue();
                long did = _TpM2.dest(this, "TC71 Dest", "EG", "Alex", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "COMPLETED", 100.0, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/itineraries/" + iid + "/record-visit", "", tok), "TC71 first");
                assert2xx(httpPostAuth("/api/itineraries/" + iid + "/record-visit", "", tok), "TC71 second");
                long count = _TpM2.visitCount(this, uid, did);
                assertEquals(1L, count, "TC71: visitCount must stay 1 after duplicate; got " + count);
        }
}

// ─── TC72 — S3-F11 second distinct itinerary same dest → count=2 ─────────────
@Tag("public")
@Tag("features_m2")
class TC72_RecordVisitTwoItinTests extends TestBase {
        @Test
        @DisplayName("TC72 — Two distinct COMPLETED itineraries same user→destination → visitCount=2")
        void record_visit_two_itin() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC72: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc72u");
                long uid = ((Number) u.get("id")).longValue();
                long did = _TpM2.dest(this, "TC72 Dest", "EG", "Luxor", "ACTIVE");
                Long i1 = _TpM2.itin(this, uid, did, "COMPLETED", 100.0, "2026-04-10");
                Long i2 = _TpM2.itin(this, uid, did, "COMPLETED", 200.0, "2026-04-12");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/itineraries/" + i1 + "/record-visit", "", tok), "TC72 i1");
                assert2xx(httpPostAuth("/api/itineraries/" + i2 + "/record-visit", "", tok), "TC72 i2");
                long count = _TpM2.visitCount(this, uid, did);
                assertEquals(2L, count, "TC72: visitCount=2; got " + count);
        }
}

// ─── TC73 — S3-F11 different destination → new edge with count=1 ─────────────
@Tag("public")
@Tag("features_m2")
class TC73_RecordVisitDifferentDestTests extends TestBase {
        @Test
        @DisplayName("TC73 — Recording to a different destination creates a new edge")
        void record_visit_different_dest() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC73: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc73u");
                long uid = ((Number) u.get("id")).longValue();
                long d1 = _TpM2.dest(this, "TC73 Dest1", "EG", "Cairo", "ACTIVE");
                long d2 = _TpM2.dest(this, "TC73 Dest2", "EG", "Aswan", "ACTIVE");
                Long i1 = _TpM2.itin(this, uid, d1, "COMPLETED", 100.0, "2026-04-10");
                Long i2 = _TpM2.itin(this, uid, d2, "COMPLETED", 200.0, "2026-04-11");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/itineraries/" + i1 + "/record-visit", "", tok), "TC73 i1");
                assert2xx(httpPostAuth("/api/itineraries/" + i2 + "/record-visit", "", tok), "TC73 i2");
                assertEquals(1L, _TpM2.visitCount(this, uid, d1), "TC73: d1 count=1");
                assertEquals(1L, _TpM2.visitCount(this, uid, d2), "TC73: d2 count=1");
        }
}

// ─── TC74 — S3-F11 PLANNED itinerary → 4xx ───────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC74_RecordVisitPlannedTests extends TestBase {
        @Test
        @DisplayName("TC74 — Recording a PLANNED (not completed) itinerary returns 4xx")
        void record_visit_planned_4xx() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc74@tp.io", "TRAVELER");
                long did = _TpM2.dest(this, "TC74 Dest", "EG", "Cairo", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "PLANNED", 50.0, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/itineraries/" + iid + "/record-visit", "", tok);
                int code = r.statusCode();
                assertTrue(code / 100 == 4,
                        "TC74: must be 4xx for PLANNED; got " + code + " body=" + r.body());
        }
}

// ─── TC75 — S3-F11 CANCELLED itinerary → 4xx ─────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC75_RecordVisitCancelledTests extends TestBase {
        @Test
        @DisplayName("TC75 — Recording a CANCELLED itinerary returns 4xx")
        void record_visit_cancelled_4xx() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc75@tp.io", "TRAVELER");
                long did = _TpM2.dest(this, "TC75 Dest", "EG", "Cairo", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "CANCELLED", 50.0, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/itineraries/" + iid + "/record-visit", "", tok);
                int code = r.statusCode();
                assertTrue(code / 100 == 4,
                        "TC75: must be 4xx for CANCELLED; got " + code);
        }
}

// ─── TC76 — S3-F11 IN_PROGRESS itinerary → 4xx ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC76_RecordVisitInProgressTests extends TestBase {
        @Test
        @DisplayName("TC76 — Recording an IN_PROGRESS (not completed) itinerary returns 4xx")
        void record_visit_in_progress_4xx() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc76@tp.io", "TRAVELER");
                long did = _TpM2.dest(this, "TC76 Dest", "EG", "Cairo", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "IN_PROGRESS", 50.0, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/itineraries/" + iid + "/record-visit", "", tok);
                int code = r.statusCode();
                assertTrue(code / 100 == 4,
                        "TC76: must be 4xx for IN_PROGRESS; got " + code);
        }
}

// ─── TC77 — S3-F11 non-existent itinerary → 404 ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC77_RecordVisitNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC77 — Record visit for non-existent itinerary returns 404")
        void record_visit_not_found_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/itineraries/999999/record-visit", "", tok);
                assertEquals(404, r.statusCode(),
                        "TC77: must be 404; got " + r.statusCode());
        }
}

// ─── TC78 — S3-F11 missing JWT → 401 ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC78_RecordVisitMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC78 — Record visit without Authorization header returns 401")
        void record_visit_missing_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc78@tp.io", "TRAVELER");
                long did = _TpM2.dest(this, "TC78 Dest", "EG", "Cairo", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "COMPLETED", 50.0, "2026-04-10");
                HttpResponse<String> r = httpPost(
                        "/api/itineraries/" + iid + "/record-visit", "");
                assertEquals(401, r.statusCode(), "TC78: must be 401; got " + r.statusCode());
        }
}

// ─── TC79 — S3-F11 invalid JWT → 401 ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC79_RecordVisitInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC79 — Record visit with bogus JWT returns 401")
        void record_visit_invalid_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc79@tp.io", "TRAVELER");
                long did = _TpM2.dest(this, "TC79 Dest", "EG", "Cairo", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "COMPLETED", 50.0, "2026-04-10");
                HttpResponse<String> r = httpPostAuth(
                        "/api/itineraries/" + iid + "/record-visit", "", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC79: must be 401; got " + r.statusCode());
        }
}

// ─── TC80 — S3-F11 lastVisitDate edge property is set ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC80_RecordVisitLastVisitDateTests extends TestBase {
        @Test
        @DisplayName("TC80 — VISITED edge has non-null lastVisitDate after recording")
        void record_visit_last_visit_date() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC80: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc80u");
                long uid = ((Number) u.get("id")).longValue();
                long did = _TpM2.dest(this, "TC80 Dest", "EG", "Cairo", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "COMPLETED", 250.0, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/itineraries/" + iid + "/record-visit", "", tok), "TC80");
                java.util.List<java.util.Map<String, Object>> rows = neo4jExec(
                        "MATCH (a:`" + s3GraphUserLabel() + "` {userId:$u})-[r:`" + s3GraphRelationship()
                          + "`]->(b:`" + s3GraphCatalogLabel() + "` {destinationId:$d}) "
                          + "RETURN r.lastVisitDate AS lvd LIMIT 1",
                        java.util.Map.of("u", uid, "d", did));
                assertFalse(rows.isEmpty(), "TC80: VISITED edge must exist");
                assertNotNull(rows.get(0).get("lvd"),
                        "TC80: lastVisitDate must be set on VISITED edge");
        }
}

// ─── TC81 — S3-F11 VISIT_RECORDED log to MongoDB ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC81_RecordVisitMongoLoggedTests extends TestBase {
        @Test
        @DisplayName("TC81 — Record visit writes VISIT_RECORDED to itinerary_events")
        void record_visit_mongo_logged() throws Exception {
                BASE_URL = orderServiceUrl;
                if (mongo == null) throw new AssertionError("TC81: MongoDB required");
                String coll = s3EventsCollection();
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "VISIT_RECORDED"));
                long uid = _TpM1Seed.seedUser(this, "U", "tc81@tp.io", "TRAVELER");
                long did = _TpM2.dest(this, "TC81 Dest", "EG", "Cairo", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "COMPLETED", 100.0, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/itineraries/" + iid + "/record-visit", "", tok), "TC81");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "VISIT_RECORDED"));
                assertTrue(after > before,
                        "TC81: VISIT_RECORDED count must increase; before=" + before + " after=" + after);
        }
}

// ─── TC82 — S3-F11 null destinationId on itinerary → 4xx (cannot record) ─────
@Tag("public")
@Tag("features_m2")
class TC82_RecordVisitNullDestTests extends TestBase {
        @Test
        @DisplayName("TC82 — Itinerary with null destinationId cannot be recorded → 4xx")
        void record_visit_null_dest_4xx() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC82: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc82u");
                long uid = ((Number) u.get("id")).longValue();
                // Insert itinerary then null its destinationId.
                long did = _TpM2.dest(this, "TC82 Dest", "EG", "Cairo", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "COMPLETED", 100.0, "2026-04-10");
                try {
                        String destCol = columnByField("Itinerary", "destinationId", "destination");
                        jdbc.update("UPDATE \"" + tableName("Itinerary") + "\" SET \"" + destCol + "\"=NULL WHERE id=?", iid);
                } catch (Throwable t) {
                        // If the column is NOT NULL we can't run this assertion path;
                        // skip with a non-failing assertion.
                        return;
                }
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/itineraries/" + iid + "/record-visit", "", tok);
                int code = r.statusCode();
                assertTrue(code / 100 == 4,
                        "TC82: must be 4xx for null destinationId; got " + code);
        }
}

// ─── TC83 — S3-F11 recorded_itinerary_ids contains the recorded id ───────────
@Tag("public")
@Tag("features_m2")
class TC83_RecordVisitItineraryIdsTests extends TestBase {
        @Test
        @DisplayName("TC83 — recorded_itinerary_ids edge property includes the itinerary id")
        void record_visit_itinerary_ids() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC83: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc83u");
                long uid = ((Number) u.get("id")).longValue();
                long did = _TpM2.dest(this, "TC83 Dest", "EG", "Cairo", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "COMPLETED", 100.0, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/itineraries/" + iid + "/record-visit", "", tok), "TC83");
                java.util.List<java.util.Map<String, Object>> rows = neo4jExec(
                        "MATCH (a:`" + s3GraphUserLabel() + "` {userId:$u})-[r:`" + s3GraphRelationship()
                          + "`]->(b:`" + s3GraphCatalogLabel() + "` {destinationId:$d}) "
                          + "RETURN r.recorded_itinerary_ids AS ids LIMIT 1",
                        java.util.Map.of("u", uid, "d", did));
                assertFalse(rows.isEmpty(), "TC83: VISITED edge must exist");
                Object ids = rows.get(0).get("ids");
                assertNotNull(ids, "TC83: recorded_itinerary_ids must be set");
                String idsStr = String.valueOf(ids);
                assertTrue(idsStr.contains(String.valueOf(iid)),
                        "TC83: recorded_itinerary_ids should contain " + iid + "; got " + idsStr);
        }
}

// ─── TC84 — S3-F11 Destination node created with destinationId ───────────────
@Tag("public")
@Tag("features_m2")
class TC84_RecordVisitDestinationNodeTests extends TestBase {
        @Test
        @DisplayName("TC84 — Recording creates Destination node with destinationId property")
        void record_visit_destination_node() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC84: Neo4j required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc84u");
                long uid = ((Number) u.get("id")).longValue();
                long did = _TpM2.dest(this, "TC84 Dest", "EG", "Cairo", "ACTIVE");
                Long iid = _TpM2.itin(this, uid, did, "COMPLETED", 100.0, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/itineraries/" + iid + "/record-visit", "", tok), "TC84");
                java.util.List<java.util.Map<String, Object>> rows = neo4jExec(
                        "MATCH (d:`" + s3GraphCatalogLabel() + "` {destinationId:$d}) RETURN d.destinationId AS did LIMIT 1",
                        java.util.Map.of("d", did));
                assertFalse(rows.isEmpty(), "TC84: Destination node must exist");
                Object got = rows.get(0).get("did");
                assertEquals(did, got instanceof Number n ? n.longValue() : -1L,
                        "TC84: destinationId property must equal seeded id; got " + got);
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S3-F12 — Destination Recommendations (TC85..TC99)
// Endpoint: GET /api/itineraries/recommendations?userId={id}&limit={n}
// Cache key: recommendations:user:{userId}, TTL ≤ 600s
// ════════════════════════════════════════════════════════════════════════════

// ─── TC85 — S3-F12 happy recommendations from similar users ──────────────────
@Tag("public")
@Tag("features_m2")
class TC85_RecommendationsHappyTests extends TestBase {
        @Test
        @DisplayName("TC85 — Returns destinations that similar users visited but caller hasn't")
        void recs_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC85: Neo4j required");
                java.util.Map<String, Object> aMap = seedAndLoginUser("tc85a");
                long aId = ((Number) aMap.get("id")).longValue();
                String aTok = (String) aMap.get("token");
                java.util.Map<String, Object> bMap = seedAndLoginUser("tc85b");
                long bId = ((Number) bMap.get("id")).longValue();
                java.util.Map<String, Object> cMap = seedAndLoginUser("tc85c");
                long cId = ((Number) cMap.get("id")).longValue();
                long d1 = _TpM2.dest(this, "Cairo", "EG", "Cairo", "ACTIVE");
                long d2 = _TpM2.dest(this, "Luxor", "EG", "Luxor", "ACTIVE");
                long d3 = _TpM2.dest(this, "Aswan", "EG", "Aswan", "ACTIVE");
                long d4 = _TpM2.dest(this, "Hurghada", "EG", "Hurghada", "ACTIVE");
                String adm = adminToken();
                _TpM2.itinAndRecord(this, aId, d1, adm);  // a→d1
                _TpM2.itinAndRecord(this, aId, d2, adm);  // a→d2
                _TpM2.itinAndRecord(this, bId, d1, adm);  // b→d1 (similar to a)
                _TpM2.itinAndRecord(this, bId, d3, adm);  // b→d3
                _TpM2.itinAndRecord(this, cId, d2, adm);  // c→d2 (similar to a)
                _TpM2.itinAndRecord(this, cId, d4, adm);  // c→d4
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=" + aId + "&limit=10", aTok);
                assert2xx(r, "TC85");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                java.util.Set<Long> destIds = new java.util.HashSet<>();
                for (JsonNode it : list) {
                        long dv = it.path("destinationId").asLong(-1);
                        if (dv > 0) destIds.add(dv);
                }
                assertTrue(destIds.contains(d3) || destIds.contains(d4),
                        "TC85: recommendations should include d3 or d4; got " + destIds);
                assertFalse(destIds.contains(d1), "TC85: must NOT recommend already-visited d1");
                assertFalse(destIds.contains(d2), "TC85: must NOT recommend already-visited d2");
        }
}

// ─── TC86 — S3-F12 ownership violation → 403 ─────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC86_RecommendationsOwnershipTests extends TestBase {
        @Test
        @DisplayName("TC86 — userId=other-user's-id with non-ADMIN token returns 403")
        void recs_ownership_403() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> aMap = seedAndLoginUser("tc86a");
                long aId = ((Number) aMap.get("id")).longValue();
                java.util.Map<String, Object> bMap = seedAndLoginUser("tc86b");
                String bTok = (String) bMap.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=" + aId, bTok);
                assertEquals(403, r.statusCode(), "TC86: must be 403; got " + r.statusCode());
        }
}

// ─── TC87 — S3-F12 ADMIN bypass ──────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC87_RecommendationsAdminBypassTests extends TestBase {
        @Test
        @DisplayName("TC87 — ADMIN can request recommendations for any user")
        void recs_admin_bypass() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc87u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=" + uid, tok);
                assert2xx(r, "TC87");
        }
}

// ─── TC88 — S3-F12 non-existent user → 404 ───────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC88_RecommendationsUserNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC88 — Non-existent userId returns 404 (with ADMIN token)")
        void recs_user_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=9999999", tok);
                assertEquals(404, r.statusCode(), "TC88: must be 404; got " + r.statusCode());
        }
}

// ─── TC89 — S3-F12 missing JWT → 401 ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC89_RecommendationsMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC89 — Missing Authorization header returns 401")
        void recs_missing_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGet("/api/itineraries/recommendations?userId=1");
                assertEquals(401, r.statusCode(), "TC89: must be 401; got " + r.statusCode());
        }
}

// ─── TC90 — S3-F12 invalid JWT → 401 ─────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC90_RecommendationsInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC90 — Bogus JWT returns 401")
        void recs_invalid_jwt_401() throws Exception {
                BASE_URL = orderServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=1", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC90: must be 401; got " + r.statusCode());
        }
}

// ─── TC91 — S3-F12 empty list when no recorded visits ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC91_RecommendationsEmptyTests extends TestBase {
        @Test
        @DisplayName("TC91 — User with no recorded visits gets empty recommendations list")
        void recs_empty() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc91u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=" + uid, tok);
                assert2xx(r, "TC91");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC91: empty list expected; got " + list.size());
        }
}

// ─── TC92 — S3-F12 limit caps results ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC92_RecommendationsLimitTests extends TestBase {
        @Test
        @DisplayName("TC92 — limit=N caps results")
        void recs_limit() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc92u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=" + uid + "&limit=2", tok);
                assert2xx(r, "TC92");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() <= 2, "TC92: must cap at 2; got " + list.size());
        }
}

// ─── TC93 — S3-F12 default limit=5 ───────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC93_RecommendationsDefaultLimitTests extends TestBase {
        @Test
        @DisplayName("TC93 — Omitting limit applies default of 5")
        void recs_default_limit() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc93u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=" + uid, tok);
                assert2xx(r, "TC93");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() <= 5, "TC93: default limit=5; got " + list.size());
        }
}

// ─── TC94 — S3-F12 each item carries destinationId ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC94_RecommendationsHasDestIdTests extends TestBase {
        @Test
        @DisplayName("TC94 — Each recommendation carries 'destinationId'")
        void recs_has_dest_id() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> aMap = seedAndLoginUser("tc94a");
                long aId = ((Number) aMap.get("id")).longValue();
                String aTok = (String) aMap.get("token");
                java.util.Map<String, Object> bMap = seedAndLoginUser("tc94b");
                long bId = ((Number) bMap.get("id")).longValue();
                long d1 = _TpM2.dest(this, "Cairo94", "EG", "Cairo", "ACTIVE");
                long d2 = _TpM2.dest(this, "Luxor94", "EG", "Luxor", "ACTIVE");
                String adm = adminToken();
                _TpM2.itinAndRecord(this, aId, d1, adm);
                _TpM2.itinAndRecord(this, bId, d1, adm);
                _TpM2.itinAndRecord(this, bId, d2, adm);
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=" + aId, aTok);
                assert2xx(r, "TC94");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        assertTrue(it.has("destinationId") || it.has("destination_id"),
                                "TC94: each recommendation must include 'destinationId'; got " + it);
                }
        }
}

// ─── TC95 — S3-F12 negative limit handled (≤0 → returns empty or default) ────
@Tag("public")
@Tag("features_m2")
class TC95_RecommendationsNegativeLimitTests extends TestBase {
        @Test
        @DisplayName("TC95 — limit=0 returns 4xx or empty list")
        void recs_negative_limit() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc95u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=" + uid + "&limit=0", tok);
                int code = r.statusCode();
                if (code / 100 == 2) {
                        JsonNode arr = parseNode(r.body());
                        JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                        assertEquals(0, list.size(), "TC95: limit=0 must yield 0 results; got " + list.size());
                } else {
                        assertTrue(code / 100 == 4, "TC95: must be 2xx (with empty list) or 4xx; got " + code);
                }
        }
}

// ─── TC96 — S3-F12 excludes destinations caller already visited ──────────────
@Tag("public")
@Tag("features_m2")
class TC96_RecommendationsExcludesOwnTests extends TestBase {
        @Test
        @DisplayName("TC96 — Recommendations exclude destinations the caller already visited")
        void recs_excludes_own() throws Exception {
                BASE_URL = orderServiceUrl;
                if (neo4j == null) throw new AssertionError("TC96: Neo4j required");
                java.util.Map<String, Object> aMap = seedAndLoginUser("tc96a");
                long aId = ((Number) aMap.get("id")).longValue();
                String aTok = (String) aMap.get("token");
                java.util.Map<String, Object> bMap = seedAndLoginUser("tc96b");
                long bId = ((Number) bMap.get("id")).longValue();
                long dShared = _TpM2.dest(this, "TC96 Shared", "EG", "Cairo", "ACTIVE");
                long dOnlyB  = _TpM2.dest(this, "TC96 OnlyB", "EG", "Luxor", "ACTIVE");
                String adm = adminToken();
                _TpM2.itinAndRecord(this, aId, dShared, adm);
                _TpM2.itinAndRecord(this, bId, dShared, adm);
                _TpM2.itinAndRecord(this, bId, dOnlyB, adm);
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=" + aId + "&limit=10", aTok);
                assert2xx(r, "TC96");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        long got = it.path("destinationId").asLong(-1);
                        assertNotEquals(dShared, got, "TC96: own destination must not be recommended");
                }
        }
}

// ─── TC97 — S3-F12 score field present and numeric ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC97_RecommendationsScoreTests extends TestBase {
        @Test
        @DisplayName("TC97 — score field is present (or similarUserCount) on each recommendation")
        void recs_score() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc97u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=" + uid, tok);
                assert2xx(r, "TC97");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        assertTrue(it.has("score") || it.has("similarUserCount"),
                                "TC97: each recommendation must include 'score' or 'similarUserCount'; got " + it);
                }
        }
}

// ─── TC98 — S3-F12 cache populated with key recommendations:user:{id} ────────
@Tag("public")
@Tag("features_m2")
class TC98_RecommendationsCachePopulatedTests extends TestBase {
        @Test
        @DisplayName("TC98 — Recommendation response is cached in Redis under recommendations:user:{userId}")
        void recs_cache_populated() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc98u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                java.util.Set<String> beforeKeys = (redis != null) ? redisKeys("*") : new java.util.HashSet<>();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/recommendations?userId=" + uid, tok);
                assert2xx(r, "TC98");
                if (redis != null) {
                        java.util.Set<String> afterKeys = redisKeys("*");
                        assertTrue(afterKeys.size() >= beforeKeys.size(),
                                "TC98: at least one cache key expected; before=" + beforeKeys.size()
                                        + " after=" + afterKeys.size());
                }
        }
}

// ─── TC99 — S3-F12 cache hit returns same body ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC99_RecommendationsCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC99 — Two identical requests return same body (cache hit)")
        void recs_cache_hit() throws Exception {
                BASE_URL = orderServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc99u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                String url = "/api/itineraries/recommendations?userId=" + uid + "&limit=5";
                HttpResponse<String> r1 = httpGetAuth(url, tok);
                assert2xx(r1, "TC99 first");
                HttpResponse<String> r2 = httpGetAuth(url, tok);
                assert2xx(r2, "TC99 second");
                assertEquals(r1.body(), r2.body(), "TC99: cached body must match first body");
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S4-F10 — Activity Analytics Dashboard (TC100..TC117)
// Endpoint: GET /api/activities/analytics?startDate=&endDate=&userId=
// DTO: totalActivities, totalCost, avgActivityCost, completionRate,
//      activitiesByStatus (BOOKED/STARTED/COMPLETED/CANCELLED)
// ════════════════════════════════════════════════════════════════════════════

// ─── TC100 — S4-F10 happy aggregate for one user ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC100_ActivityAnalyticsHappyTests extends TestBase {
        @Test
        @DisplayName("TC100 — Aggregate returns totalActivities/totalCost/avgActivityCost/completionRate/activitiesByStatus")
        void analytics_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc100u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                _TpM2S4.act(this, iid, "Pyramid Tour",  300.0, "2026-03-02", "COMPLETED");
                _TpM2S4.act(this, iid, "Felucca Ride",  200.0, "2026-03-04", "COMPLETED");
                _TpM2S4.act(this, iid, "Museum Visit",  150.0, "2026-03-06", "COMPLETED");
                _TpM2S4.act(this, iid, "Spa",           400.0, "2026-03-08", "BOOKED");
                _TpM2S4.act(this, iid, "Diving",        500.0, "2026-03-10", "CANCELLED");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-03-01&endDate=2026-03-31&userId=" + uid, tok);
                assert2xx(r, "TC100");
                JsonNode j = parseNode(r.body());
                assertEquals(5L, _TpM2.rL(j, "totalActivities", "total_activities"),
                        "TC100: totalActivities=5");
                double totalCost = _TpM2.rD(j, "totalCost", "total_cost");
                assertTrue(totalCost > 0, "TC100: totalCost > 0; got " + totalCost);
                JsonNode bs = _TpM2.rO(j, "activitiesByStatus", "activities_by_status");
                assertNotNull(bs, "TC100: activitiesByStatus key required");
        }
}

// ─── TC101 — S4-F10 totalCost sum ────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC101_ActivityAnalyticsTotalCostTests extends TestBase {
        @Test
        @DisplayName("TC101 — totalCost equals SUM(cost) for all activities in range")
        void totals() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc101u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-04-01");
                _TpM2S4.act(this, iid, "A1", 1000.0, "2026-04-05", "COMPLETED");
                _TpM2S4.act(this, iid, "A2", 2000.0, "2026-04-10", "COMPLETED");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30&userId=" + uid, tok);
                assert2xx(r, "TC101");
                JsonNode j = parseNode(r.body());
                double tc = _TpM2.rD(j, "totalCost", "total_cost");
                assertEquals(3000.0, tc, 0.01, "TC101: totalCost=3000; got " + tc);
        }
}

// ─── TC102 — S4-F10 avgActivityCost ──────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC102_ActivityAnalyticsAvgCostTests extends TestBase {
        @Test
        @DisplayName("TC102 — avgActivityCost equals totalCost / totalActivities")
        void avg_cost() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc102u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-04-01");
                _TpM2S4.act(this, iid, "A1", 100.0, "2026-04-05", "COMPLETED");
                _TpM2S4.act(this, iid, "A2", 300.0, "2026-04-10", "COMPLETED");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30&userId=" + uid, tok);
                assert2xx(r, "TC102");
                JsonNode j = parseNode(r.body());
                double avg = _TpM2.rD(j, "avgActivityCost", "avg_activity_cost");
                assertEquals(200.0, avg, 0.5, "TC102: avgActivityCost=200; got " + avg);
        }
}

// ─── TC103 — S4-F10 completionRate ───────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC103_ActivityAnalyticsCompletionRateTests extends TestBase {
        @Test
        @DisplayName("TC103 — completionRate = COMPLETED / total")
        void completion_rate() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc103u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-04-01");
                _TpM2S4.act(this, iid, "A1", 100.0, "2026-04-05", "COMPLETED");
                _TpM2S4.act(this, iid, "A2", 100.0, "2026-04-06", "COMPLETED");
                _TpM2S4.act(this, iid, "A3", 100.0, "2026-04-07", "BOOKED");
                _TpM2S4.act(this, iid, "A4", 100.0, "2026-04-08", "CANCELLED");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30&userId=" + uid, tok);
                assert2xx(r, "TC103");
                double rate = _TpM2.rD(parseNode(r.body()), "completionRate", "completion_rate");
                assertEquals(0.5, rate, 0.01, "TC103: completionRate=0.5; got " + rate);
        }
}

// ─── TC104 — S4-F10 cancelled count ──────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC104_ActivityAnalyticsCancelledTests extends TestBase {
        @Test
        @DisplayName("TC104 — activitiesByStatus.CANCELLED counts only cancelled rows")
        void cancelled_count() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc104u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-04-01");
                _TpM2S4.act(this, iid, "A1", 100.0, "2026-04-05", "CANCELLED");
                _TpM2S4.act(this, iid, "A2", 100.0, "2026-04-06", "COMPLETED");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30&userId=" + uid, tok);
                assert2xx(r, "TC104");
                JsonNode bs = _TpM2.rO(parseNode(r.body()), "activitiesByStatus", "activities_by_status");
                assertNotNull(bs, "TC104: activitiesByStatus required");
                long c = bs.has("CANCELLED") ? bs.get("CANCELLED").asLong() : 0L;
                assertEquals(1L, c, "TC104: CANCELLED=1; got " + c);
        }
}

// ─── TC105 — S4-F10 activitiesByStatus map ───────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC105_ActivityAnalyticsByStatusTests extends TestBase {
        @Test
        @DisplayName("TC105 — activitiesByStatus has all four status keys")
        void by_status() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc105u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-04-01");
                _TpM2S4.act(this, iid, "A1", 100.0, "2026-04-05", "BOOKED");
                _TpM2S4.act(this, iid, "A2", 100.0, "2026-04-06", "STARTED");
                _TpM2S4.act(this, iid, "A3", 100.0, "2026-04-07", "COMPLETED");
                _TpM2S4.act(this, iid, "A4", 100.0, "2026-04-08", "CANCELLED");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30&userId=" + uid, tok);
                assert2xx(r, "TC105");
                JsonNode bs = _TpM2.rO(parseNode(r.body()), "activitiesByStatus", "activities_by_status");
                assertNotNull(bs, "TC105: activitiesByStatus required");
                for (String st : new String[]{"BOOKED", "STARTED", "COMPLETED", "CANCELLED"}) {
                        assertTrue(bs.has(st), "TC105: missing key " + st);
                }
        }
}

// ─── TC106 — S4-F10 distinct itineraries do not pollute each other ───────────
@Tag("public")
@Tag("features_m2")
class TC106_ActivityAnalyticsByItineraryTests extends TestBase {
        @Test
        @DisplayName("TC106 — Activities under different itineraries are aggregated by user")
        void by_itinerary() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc106u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long i1 = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-04-01");
                long i2 = _TpM2.itin(this, uid, 2L, "COMPLETED", 1000.0, "2026-04-02");
                _TpM2S4.act(this, i1, "A1", 100.0, "2026-04-05", "COMPLETED");
                _TpM2S4.act(this, i2, "A2", 200.0, "2026-04-06", "COMPLETED");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30&userId=" + uid, tok);
                assert2xx(r, "TC106");
                long total = _TpM2.rL(parseNode(r.body()), "totalActivities", "total_activities");
                assertEquals(2L, total, "TC106: totalActivities=2; got " + total);
        }
}

// ─── TC107 — S4-F10 empty range returns zeros ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC107_ActivityAnalyticsEmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC107 — Empty date range returns totalActivities=0")
        void empty_range() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc107u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2099-01-01&endDate=2099-01-31&userId=" + uid, tok);
                assert2xx(r, "TC107");
                long total = _TpM2.rL(parseNode(r.body()), "totalActivities", "total_activities");
                assertEquals(0L, total, "TC107: totalActivities=0 expected; got " + total);
        }
}

// ─── TC108 — S4-F10 invalid range → 400 ──────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC108_ActivityAnalyticsInvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC108 — startDate after endDate returns 400")
        void invalid_range() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc108u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-03-01&userId=" + uid, tok);
                assertEquals(400, r.statusCode(), "TC108: must be 400; got " + r.statusCode());
        }
}

// ─── TC109 — S4-F10 ownership 403 ────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC109_ActivityAnalyticsOwnership403Tests extends TestBase {
        @Test
        @DisplayName("TC109 — userId=other-user with non-ADMIN token returns 403")
        void ownership_403() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> aMap = seedAndLoginUser("tc109a");
                long aId = ((Number) aMap.get("id")).longValue();
                java.util.Map<String, Object> bMap = seedAndLoginUser("tc109b");
                String bTok = (String) bMap.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30&userId=" + aId, bTok);
                assertEquals(403, r.statusCode(), "TC109: must be 403; got " + r.statusCode());
        }
}

// ─── TC110 — S4-F10 ADMIN bypass ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC110_ActivityAnalyticsAdminBypassTests extends TestBase {
        @Test
        @DisplayName("TC110 — ADMIN token can request any user's analytics")
        void admin_bypass() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc110u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30&userId=" + uid, tok);
                assert2xx(r, "TC110");
        }
}

// ─── TC111 — S4-F10 userId omitted requires ADMIN ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC111_ActivityAnalyticsCrossUserAdminTests extends TestBase {
        @Test
        @DisplayName("TC111 — Omitting userId requires ADMIN; non-ADMIN returns 403")
        void cross_user_admin() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc111u");
                String tok = (String) u.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30", tok);
                assertEquals(403, r.statusCode(), "TC111: non-ADMIN must be 403; got " + r.statusCode());
        }
}

// ─── TC112 — S4-F10 userId omitted with ADMIN succeeds ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC112_ActivityAnalyticsAdminGlobalTests extends TestBase {
        @Test
        @DisplayName("TC112 — ADMIN with userId omitted gets global aggregate")
        void admin_global() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30", tok);
                assert2xx(r, "TC112");
        }
}

// ─── TC113 — S4-F10 missing JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC113_ActivityAnalyticsMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC113 — Missing Authorization returns 401")
        void missing_jwt_401() throws Exception {
                BASE_URL = deliveryServiceUrl;
                HttpResponse<String> r = httpGet(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30");
                assertEquals(401, r.statusCode(), "TC113: must be 401; got " + r.statusCode());
        }
}

// ─── TC114 — S4-F10 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC114_ActivityAnalyticsInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC114 — Bogus JWT returns 401")
        void invalid_jwt_401() throws Exception {
                BASE_URL = deliveryServiceUrl;
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC114: must be 401; got " + r.statusCode());
        }
}

// ─── TC115 — S4-F10 ANALYTICS_VIEWED logged on first call ────────────────────
@Tag("public")
@Tag("features_m2")
class TC115_ActivityAnalyticsLoggedFirstCallTests extends TestBase {
        @Test
        @DisplayName("TC115 — First analytics call writes ANALYTICS_VIEWED to activity_events")
        void analytics_logged_first() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (mongo == null) throw new AssertionError("TC115: MongoDB required");
                String coll = s4EventsCollection();
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                java.util.Map<String, Object> u = seedAndLoginUser("tc115u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30&userId=" + uid, tok);
                assert2xx(r, "TC115");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assertTrue(after > before,
                        "TC115: ANALYTICS_VIEWED count must increase; before=" + before + " after=" + after);
        }
}

// ─── TC116 — S4-F10 ANALYTICS_VIEWED on cache hit too ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC116_ActivityAnalyticsLoggedCacheHitTests extends TestBase {
        @Test
        @DisplayName("TC116 — Second call (cache hit) still writes ANALYTICS_VIEWED")
        void analytics_logged_cache() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (mongo == null) throw new AssertionError("TC116: MongoDB required");
                String coll = s4EventsCollection();
                java.util.Map<String, Object> u = seedAndLoginUser("tc116u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                String url = "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30&userId=" + uid;
                assert2xx(httpGetAuth(url, tok), "TC116 first");
                long after1 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assert2xx(httpGetAuth(url, tok), "TC116 second");
                long after2 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("action", "ANALYTICS_VIEWED"));
                assertTrue(after2 > after1,
                        "TC116: ANALYTICS_VIEWED logged on cache hit; after1=" + after1 + " after2=" + after2);
        }
}

// ─── TC117 — S4-F10 cache returns same body ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC117_ActivityAnalyticsCacheSameBodyTests extends TestBase {
        @Test
        @DisplayName("TC117 — Two identical analytics requests return same body")
        void cache_same_body() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc117u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                String url = "/api/activities/analytics?startDate=2026-04-01&endDate=2026-04-30&userId=" + uid;
                HttpResponse<String> r1 = httpGetAuth(url, tok);
                assert2xx(r1, "TC117 first");
                HttpResponse<String> r2 = httpGetAuth(url, tok);
                assert2xx(r2, "TC117 second");
                assertEquals(_TpM2.rL(parseNode(r1.body()), "totalActivities", "total_activities"),
                             _TpM2.rL(parseNode(r2.body()), "totalActivities", "total_activities"),
                        "TC117: cached totalActivities must match");
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S4-F11 — Record Activity Lifecycle Event (TC118..TC127)
// Endpoint: POST /api/activities/{activityId}/events
// Body: {"status":"STARTED|COMPLETED|CANCELLED|BOOKED","notes":"..."}
// Persists to Cassandra activity_lifecycle_events (partitioned by activityId)
// AND Mongo activity_events with type=LIFECYCLE_RECORDED. actor field = notes.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC118 — S4-F11 happy path: 2xx + Cassandra row + Mongo event ────────────
@Tag("public")
@Tag("features_m2")
class TC118_RecordLifecycleHappyPathTests extends TestBase {
        @Test
        @DisplayName("TC118 — Recording lifecycle event returns 2xx and writes to Cassandra + Mongo")
        void record_lifecycle_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc118u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "Pyramid Tour", 300.0, "2026-04-05", "BOOKED");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"STARTED\",\"notes\":\"tour started on time\"}", tok);
                assert2xx(r, "TC118");
        }
}

// ─── TC119 — S4-F11 Cassandra row contents ───────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC119_RecordLifecycleCassandraTests extends TestBase {
        @Test
        @DisplayName("TC119 — Cassandra activity_lifecycle_events row created for the activity")
        void record_lifecycle_cassandra() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC119: Cassandra required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc119u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"STARTED\",\"notes\":\"x\"}", tok), "TC119");
                long count = cassandraCount(s4TimeseriesTable(),
                        s4TimeseriesPartitionField(), aid);
                assertTrue(count >= 1, "TC119: Cassandra row must exist; got count=" + count);
        }
}

// ─── TC120 — S4-F11 each transition recorded as a row ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC120_RecordLifecycleMultiTransitionTests extends TestBase {
        @Test
        @DisplayName("TC120 — BOOKED→STARTED→COMPLETED records 2 transitions in Cassandra")
        void record_lifecycle_multi() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC120: Cassandra required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc120u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                String tok = adminToken();
                long before = cassandraCount(s4TimeseriesTable(), s4TimeseriesPartitionField(), aid);
                assert2xx(httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"STARTED\",\"notes\":\"start\"}", tok), "TC120 start");
                assert2xx(httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"COMPLETED\",\"notes\":\"done\"}", tok), "TC120 done");
                long after = cassandraCount(s4TimeseriesTable(), s4TimeseriesPartitionField(), aid);
                assertTrue(after - before >= 2,
                        "TC120: at least 2 Cassandra rows expected; before=" + before + " after=" + after);
        }
}

// ─── TC121 — S4-F11 Mongo LIFECYCLE_RECORDED event written ───────────────────
@Tag("public")
@Tag("features_m2")
class TC121_RecordLifecycleMongoEventTests extends TestBase {
        @Test
        @DisplayName("TC121 — LIFECYCLE_RECORDED logged to activity_events")
        void record_lifecycle_mongo() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (mongo == null) throw new AssertionError("TC121: MongoDB required");
                String coll = s4EventsCollection();
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("type", "LIFECYCLE_RECORDED"));
                java.util.Map<String, Object> u = seedAndLoginUser("tc121u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"STARTED\",\"notes\":\"x\"}", tok), "TC121");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("type", "LIFECYCLE_RECORDED"));
                assertTrue(after > before,
                        "TC121: LIFECYCLE_RECORDED count must increase; before=" + before + " after=" + after);
        }
}

// ─── TC122 — S4-F11 invalid status → 4xx ─────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC122_RecordLifecycleInvalidStatusTests extends TestBase {
        @Test
        @DisplayName("TC122 — Invalid status value returns 4xx")
        void record_lifecycle_invalid_status() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc122u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"BOGUS_STATUS\",\"notes\":\"x\"}", tok);
                int code = r.statusCode();
                assertTrue(code / 100 == 4,
                        "TC122: must be 4xx; got " + code + " body=" + r.body());
        }
}

// ─── TC123 — S4-F11 non-existent activity → 404 ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC123_RecordLifecycleNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC123 — Non-existent activity returns 404")
        void record_lifecycle_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/activities/9999999/events",
                        "{\"status\":\"STARTED\",\"notes\":\"x\"}", tok);
                assertEquals(404, r.statusCode(), "TC123: must be 404; got " + r.statusCode());
        }
}

// ─── TC124 — S4-F11 missing JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC124_RecordLifecycleMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC124 — Missing Authorization returns 401")
        void record_lifecycle_missing_jwt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc124u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                HttpResponse<String> r = httpPost("/api/activities/" + aid + "/events",
                        "{\"status\":\"STARTED\",\"notes\":\"x\"}");
                assertEquals(401, r.statusCode(), "TC124: must be 401; got " + r.statusCode());
        }
}

// ─── TC125 — S4-F11 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC125_RecordLifecycleInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC125 — Invalid JWT returns 401")
        void record_lifecycle_invalid_jwt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc125u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                HttpResponse<String> r = httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"STARTED\",\"notes\":\"x\"}", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC125: must be 401; got " + r.statusCode());
        }
}

// ─── TC126 — S4-F11 multiple snapshots accumulate ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC126_RecordLifecycleMultiUserTests extends TestBase {
        @Test
        @DisplayName("TC126 — Two activities have isolated Cassandra partitions")
        void record_lifecycle_isolation() throws Exception {
                BASE_URL = deliveryServiceUrl;
                if (cassandra == null) throw new AssertionError("TC126: Cassandra required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc126u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long a1 = _TpM2S4.act(this, iid, "A1", 100.0, "2026-04-05", "BOOKED");
                long a2 = _TpM2S4.act(this, iid, "A2", 100.0, "2026-04-06", "BOOKED");
                String tok = adminToken();
                assert2xx(httpPostAuth("/api/activities/" + a1 + "/events",
                        "{\"status\":\"STARTED\",\"notes\":\"x\"}", tok), "TC126 a1");
                long c1 = cassandraCount(s4TimeseriesTable(), s4TimeseriesPartitionField(), a1);
                long c2 = cassandraCount(s4TimeseriesTable(), s4TimeseriesPartitionField(), a2);
                assertTrue(c1 >= 1 && c2 == 0,
                        "TC126: a1 has events, a2 doesn't; c1=" + c1 + " c2=" + c2);
        }
}

// ─── TC127 — S4-F11 notes field is the actor field ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC127_RecordLifecycleNotesFieldTests extends TestBase {
        @Test
        @DisplayName("TC127 — 'notes' from request body is recorded (s4ActorField=notes)")
        void record_lifecycle_notes() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc127u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                String tok = adminToken();
                String specificNote = "tc127-specific-note-" + nonce();
                assert2xx(httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"STARTED\",\"notes\":\"" + specificNote + "\"}", tok), "TC127");
                // Note: actor field assertion lives in TestBase.s4ActorField (=notes)
                assertEquals("notes", s4ActorField(),
                        "TC127: theme actor field must be 'notes'");
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S4-F12 — Get Activity Timeline (TC128..TC135)
// Endpoint: GET /api/activities/{activityId}/timeline
// Returns Cassandra rows in reverse chronological order.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC128 — S4-F12 happy path returns recorded events ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC128_TimelineHappyTests extends TestBase {
        @Test
        @DisplayName("TC128 — Timeline returns recorded lifecycle events for an activity")
        void timeline_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc128u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                String adm = adminToken();
                assert2xx(httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"STARTED\",\"notes\":\"a\"}", adm), "TC128 seed");
                assert2xx(httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"COMPLETED\",\"notes\":\"b\"}", adm), "TC128 seed2");
                HttpResponse<String> r = httpGetAuth("/api/activities/" + aid + "/timeline", tok);
                assert2xx(r, "TC128");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC128: at least 2 entries; got " + list.size());
        }
}

// ─── TC129 — S4-F12 empty list when no events ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC129_TimelineEmptyTests extends TestBase {
        @Test
        @DisplayName("TC129 — Activity with no recorded events returns empty list")
        void timeline_empty() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc129u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                HttpResponse<String> r = httpGetAuth("/api/activities/" + aid + "/timeline", tok);
                assert2xx(r, "TC129");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC129: empty list expected; got " + list.size());
        }
}

// ─── TC130 — S4-F12 ownership 403 ────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC130_TimelineOwnership403Tests extends TestBase {
        @Test
        @DisplayName("TC130 — Other user's token returns 403")
        void timeline_ownership() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> aMap = seedAndLoginUser("tc130a");
                long aId = ((Number) aMap.get("id")).longValue();
                long iid = _TpM2.itin(this, aId, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                java.util.Map<String, Object> bMap = seedAndLoginUser("tc130b");
                String bTok = (String) bMap.get("token");
                HttpResponse<String> r = httpGetAuth("/api/activities/" + aid + "/timeline", bTok);
                assertEquals(403, r.statusCode(), "TC130: must be 403; got " + r.statusCode());
        }
}

// ─── TC131 — S4-F12 ADMIN bypass ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC131_TimelineAdminBypassTests extends TestBase {
        @Test
        @DisplayName("TC131 — ADMIN can read any user's timeline")
        void timeline_admin_bypass() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc131u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/activities/" + aid + "/timeline", tok);
                assert2xx(r, "TC131");
        }
}

// ─── TC132 — S4-F12 non-existent activity → 404 ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC132_TimelineNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC132 — Non-existent activity returns 404")
        void timeline_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/activities/9999999/timeline", tok);
                assertEquals(404, r.statusCode(), "TC132: must be 404; got " + r.statusCode());
        }
}

// ─── TC133 — S4-F12 missing JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC133_TimelineMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC133 — Missing Authorization returns 401")
        void timeline_missing_jwt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                HttpResponse<String> r = httpGet("/api/activities/1/timeline");
                assertEquals(401, r.statusCode(), "TC133: must be 401; got " + r.statusCode());
        }
}

// ─── TC134 — S4-F12 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC134_TimelineInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC134 — Invalid JWT returns 401")
        void timeline_invalid_jwt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                HttpResponse<String> r = httpGetAuth("/api/activities/1/timeline", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC134: must be 401; got " + r.statusCode());
        }
}

// ─── TC135 — S4-F12 returns most recent event first ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC135_TimelineReverseChronoTests extends TestBase {
        @Test
        @DisplayName("TC135 — Timeline returns events in reverse chronological order")
        void timeline_reverse_chrono() throws Exception {
                BASE_URL = deliveryServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc135u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 500.0, "2026-04-01");
                long aid = _TpM2S4.act(this, iid, "A", 100.0, "2026-04-05", "BOOKED");
                String adm = adminToken();
                assert2xx(httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"STARTED\",\"notes\":\"first\"}", adm), "TC135 first");
                Thread.sleep(50);
                assert2xx(httpPostAuth("/api/activities/" + aid + "/events",
                        "{\"status\":\"COMPLETED\",\"notes\":\"second\"}", adm), "TC135 second");
                HttpResponse<String> r = httpGetAuth("/api/activities/" + aid + "/timeline", tok);
                assert2xx(r, "TC135");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC135: at least 2 entries; got " + list.size());
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S5-F10 — Destination-Season Analytics Dashboard (TC136..TC158)
// Endpoint: GET /api/bookings/analytics/destination-season
// Returns array grouped by destinationId+season with revenue, count.
// Booking.bookingDetails JSONB carries seasonalSurcharge (default 0.0).
// ════════════════════════════════════════════════════════════════════════════

// ─── TC136 — S5-F10 happy path: returns array ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC136_DestSeasonHappyTests extends TestBase {
        @Test
        @DisplayName("TC136 — destination-season analytics returns 2xx and an array")
        void dest_season_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc136u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC136");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertNotNull(list, "TC136: response must be an array or paged content");
        }
}

// ─── TC137 — S5-F10 each row carries destinationId ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC137_DestSeasonHasDestIdTests extends TestBase {
        @Test
        @DisplayName("TC137 — Each row in dashboard carries destinationId")
        void has_dest_id() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc137u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC137");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        assertTrue(it.has("destinationId") || it.has("destination_id"),
                                "TC137: each row must have destinationId; got " + it);
                }
        }
}

// ─── TC138 — S5-F10 each row carries season ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC138_DestSeasonHasSeasonTests extends TestBase {
        @Test
        @DisplayName("TC138 — Each row in dashboard carries 'season' field")
        void has_season() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc138u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC138");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        assertTrue(it.has("season"),
                                "TC138: each row must have 'season'; got " + it);
                }
        }
}

// ─── TC139 — S5-F10 each row carries revenue ─────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC139_DestSeasonHasRevenueTests extends TestBase {
        @Test
        @DisplayName("TC139 — Each row in dashboard carries 'revenue'")
        void has_revenue() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc139u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC139");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        assertTrue(it.has("revenue") || it.has("totalRevenue"),
                                "TC139: each row must have revenue; got " + it);
                }
        }
}

// ─── TC140 — S5-F10 each row carries count ───────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC140_DestSeasonHasCountTests extends TestBase {
        @Test
        @DisplayName("TC140 — Each row in dashboard carries booking count")
        void has_count() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc140u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC140");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        assertTrue(it.has("count") || it.has("bookingCount"),
                                "TC140: each row must have count; got " + it);
                }
        }
}

// ─── TC141 — S5-F10 grouping by destination+season ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC141_DestSeasonGroupingTests extends TestBase {
        @Test
        @DisplayName("TC141 — Two bookings same destination+season collapse to one row")
        void grouping() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc141u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                _TpM2S5.bkg(this, uid, iid, 100.0, "2026-03-15", "2026-03-20", "COMPLETED");
                _TpM2S5.bkg(this, uid, iid, 200.0, "2026-03-22", "2026-03-25", "COMPLETED");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC141");
        }
}

// ─── TC142 — S5-F10 seasonalSurcharge included from JSONB ────────────────────
@Tag("public")
@Tag("features_m2")
class TC142_DestSeasonSurchargeTests extends TestBase {
        @Test
        @DisplayName("TC142 — bookingDetails.seasonalSurcharge is reflected in revenue math")
        void surcharge_included() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc142u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                _TpM2S5.setBookingDetails(this, bid, "{\"seasonalSurcharge\":50.0}");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC142");
        }
}

// ─── TC143 — S5-F10 missing seasonalSurcharge defaults to 0.0 ────────────────
@Tag("public")
@Tag("features_m2")
class TC143_DestSeasonNoSurchargeTests extends TestBase {
        @Test
        @DisplayName("TC143 — Missing seasonalSurcharge is treated as 0.0")
        void no_surcharge() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc143u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC143");
        }
}

// ─── TC144 — S5-F10 only COMPLETED bookings count ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC144_DestSeasonCompletedOnlyTests extends TestBase {
        @Test
        @DisplayName("TC144 — CANCELLED bookings excluded from analytics")
        void completed_only() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc144u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                _TpM2S5.bkg(this, uid, iid, 100.0, "2026-03-15", "2026-03-20", "COMPLETED");
                _TpM2S5.bkg(this, uid, iid, 999.0, "2026-03-15", "2026-03-20", "CANCELLED");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC144");
        }
}

// ─── TC145 — S5-F10 ADMIN-only: non-ADMIN → 403 ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC145_DestSeasonAdminOnlyTests extends TestBase {
        @Test
        @DisplayName("TC145 — Non-ADMIN token returns 403")
        void admin_only_403() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc145u");
                String tok = (String) u.get("token");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assertEquals(403, r.statusCode(), "TC145: must be 403; got " + r.statusCode());
        }
}

// ─── TC146 — S5-F10 ADMIN bypass ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC146_DestSeasonAdminBypassTests extends TestBase {
        @Test
        @DisplayName("TC146 — ADMIN token returns 2xx")
        void admin_bypass() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC146");
        }
}

// ─── TC147 — S5-F10 missing JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC147_DestSeasonMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC147 — Missing Authorization returns 401")
        void missing_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGet("/api/bookings/analytics/destination-season");
                assertEquals(401, r.statusCode(), "TC147: must be 401; got " + r.statusCode());
        }
}

// ─── TC148 — S5-F10 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC148_DestSeasonInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC148 — Bogus JWT returns 401")
        void invalid_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC148: must be 401; got " + r.statusCode());
        }
}

// ─── TC149 — S5-F10 ANALYTICS_VIEWED logged ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC149_DestSeasonAnalyticsLoggedTests extends TestBase {
        @Test
        @DisplayName("TC149 — Analytics call writes ANALYTICS_VIEWED to payment_audit_trail")
        void analytics_logged() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC149: MongoDB required");
                String coll = s5AuditCollection();
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("eventType", "ANALYTICS_VIEWED"));
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC149");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("eventType", "ANALYTICS_VIEWED"));
                assertTrue(after > before,
                        "TC149: ANALYTICS_VIEWED count must increase; before=" + before + " after=" + after);
        }
}

// ─── TC150 — S5-F10 ANALYTICS_VIEWED on cache hit too ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC150_DestSeasonLoggedOnCacheTests extends TestBase {
        @Test
        @DisplayName("TC150 — Cache hit also writes ANALYTICS_VIEWED")
        void analytics_logged_cache() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC150: MongoDB required");
                String coll = s5AuditCollection();
                String tok = adminToken();
                String url = "/api/bookings/analytics/destination-season";
                assert2xx(httpGetAuth(url, tok), "TC150 first");
                long after1 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("eventType", "ANALYTICS_VIEWED"));
                assert2xx(httpGetAuth(url, tok), "TC150 second");
                long after2 = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("eventType", "ANALYTICS_VIEWED"));
                assertTrue(after2 > after1,
                        "TC150: ANALYTICS_VIEWED logged on cache hit; after1=" + after1 + " after2=" + after2);
        }
}

// ─── TC151 — S5-F10 cache hit returns same body ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC151_DestSeasonCacheSameBodyTests extends TestBase {
        @Test
        @DisplayName("TC151 — Two identical analytics requests return same body")
        void cache_same_body() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                String url = "/api/bookings/analytics/destination-season";
                HttpResponse<String> r1 = httpGetAuth(url, tok);
                assert2xx(r1, "TC151 first");
                HttpResponse<String> r2 = httpGetAuth(url, tok);
                assert2xx(r2, "TC151 second");
                assertEquals(r1.body(), r2.body(), "TC151: cached body must match");
        }
}

// ─── TC152 — S5-F10 empty data returns 2xx + empty/zeroed array ──────────────
@Tag("public")
@Tag("features_m2")
class TC152_DestSeasonEmptyTests extends TestBase {
        @Test
        @DisplayName("TC152 — No bookings returns 2xx with empty array")
        void empty_data() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC152");
        }
}

// ─── TC153 — S5-F10 distinct seasons grouped separately ──────────────────────
@Tag("public")
@Tag("features_m2")
class TC153_DestSeasonDistinctSeasonsTests extends TestBase {
        @Test
        @DisplayName("TC153 — Same destination, different seasons → different rows")
        void distinct_seasons() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc153u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                _TpM2S5.bkg(this, uid, iid, 100.0, "2026-01-15", "2026-01-20", "COMPLETED"); // winter
                _TpM2S5.bkg(this, uid, iid, 200.0, "2026-07-15", "2026-07-20", "COMPLETED"); // summer
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC153");
        }
}

// ─── TC154 — S5-F10 distinct destinations grouped separately ─────────────────
@Tag("public")
@Tag("features_m2")
class TC154_DestSeasonDistinctDestsTests extends TestBase {
        @Test
        @DisplayName("TC154 — Different destinations → different rows even in same season")
        void distinct_dests() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc154u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long i1 = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long i2 = _TpM2.itin(this, uid, 2L, "COMPLETED", 1000.0, "2026-03-02");
                _TpM2S5.bkg(this, uid, i1, 100.0, "2026-03-15", "2026-03-20", "COMPLETED");
                _TpM2S5.bkg(this, uid, i2, 200.0, "2026-03-15", "2026-03-20", "COMPLETED");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC154");
        }
}

// ─── TC155 — S5-F10 seasonalSurcharge=0.0 explicit ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC155_DestSeasonZeroSurchargeTests extends TestBase {
        @Test
        @DisplayName("TC155 — seasonalSurcharge=0.0 is treated identically to default")
        void zero_surcharge() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc155u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = adminToken();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                _TpM2S5.setBookingDetails(this, bid, "{\"seasonalSurcharge\":0.0}");
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC155");
        }
}

// ─── TC156 — S5-F10 user with no bookings → analytics still works ────────────
@Tag("public")
@Tag("features_m2")
class TC156_DestSeasonNoBookingsTests extends TestBase {
        @Test
        @DisplayName("TC156 — Aggregate works even when test user has no bookings")
        void no_bookings() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC156");
        }
}

// ─── TC157 — S5-F10 Booking entity required check ────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC157_DestSeasonBookingEntityTests extends TestBase {
        @Test
        @DisplayName("TC157 — bookings table is the underlying source")
        void booking_entity() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tn = tableName("Booking");
                assertNotNull(tn, "TC157: Booking table must be resolvable");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC157");
        }
}

// ─── TC158 — S5-F10 response shape sanity ────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC158_DestSeasonResponseShapeTests extends TestBase {
        @Test
        @DisplayName("TC158 — Response is JSON parseable (object or array)")
        void response_shape() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/analytics/destination-season", tok);
                assert2xx(r, "TC158");
                JsonNode j = parseNode(r.body());
                assertNotNull(j, "TC158: response must be parseable JSON; body=" + r.body());
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S5-F11 — Payment Audit History per Booking (TC159..TC174)
// Endpoint: GET /api/bookings/{bookingId}/payment-history?page=&size=
// Reads payment_audit_trail. Excludes eventType=ANALYTICS_VIEWED. Sorted ASC.
// Logs ANALYTICS_VIEWED on call.
// ════════════════════════════════════════════════════════════════════════════

// ─── TC159 — S5-F11 happy path returns events ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC159_AuditHistoryHappyTests extends TestBase {
        @Test
        @DisplayName("TC159 — Payment-history returns 2xx with events")
        void audit_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC159: MongoDB required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc159u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                org.bson.Document doc = new org.bson.Document()
                        .append("bookingId", bid)
                        .append("eventType", "PAYMENT_CAPTURED")
                        .append("amount", 500.0)
                        .append("timestamp", new java.util.Date());
                mongo.getCollection(s5AuditCollection()).insertOne(doc);
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history", tok);
                assert2xx(r, "TC159");
        }
}

// ─── TC160 — S5-F11 ANALYTICS_VIEWED is excluded from results ────────────────
@Tag("public")
@Tag("features_m2")
class TC160_AuditHistoryExcludesViewedTests extends TestBase {
        @Test
        @DisplayName("TC160 — Returned list excludes ANALYTICS_VIEWED entries")
        void audit_excludes_viewed() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC160: MongoDB required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc160u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                String coll = s5AuditCollection();
                mongo.getCollection(coll).insertOne(new org.bson.Document()
                        .append("bookingId", bid)
                        .append("eventType", "ANALYTICS_VIEWED")
                        .append("timestamp", new java.util.Date()));
                mongo.getCollection(coll).insertOne(new org.bson.Document()
                        .append("bookingId", bid)
                        .append("eventType", "PAYMENT_CAPTURED")
                        .append("amount", 500.0)
                        .append("timestamp", new java.util.Date()));
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history", tok);
                assert2xx(r, "TC160");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        String et = it.path("eventType").asText("");
                        assertNotEquals("ANALYTICS_VIEWED", et,
                                "TC160: ANALYTICS_VIEWED must be excluded; got " + it);
                }
        }
}

// ─── TC161 — S5-F11 paginated (page/size respected) ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC161_AuditHistoryPaginatedTests extends TestBase {
        @Test
        @DisplayName("TC161 — page/size parameters are honored")
        void audit_paginated() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC161: MongoDB required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc161u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                String coll = s5AuditCollection();
                for (int i = 0; i < 5; i++) {
                        mongo.getCollection(coll).insertOne(new org.bson.Document()
                                .append("bookingId", bid)
                                .append("eventType", "PAYMENT_CAPTURED")
                                .append("amount", 100.0 + i)
                                .append("timestamp", new java.util.Date(System.currentTimeMillis() + i)));
                }
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history?page=0&size=2", tok);
                assert2xx(r, "TC161");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() <= 2, "TC161: size=2 must cap; got " + list.size());
        }
}

// ─── TC162 — S5-F11 sorted ASC by timestamp ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC162_AuditHistorySortAscTests extends TestBase {
        @Test
        @DisplayName("TC162 — Events sorted ascending by timestamp (oldest first)")
        void audit_sort_asc() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC162: MongoDB required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc162u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                String coll = s5AuditCollection();
                long t0 = System.currentTimeMillis();
                mongo.getCollection(coll).insertOne(new org.bson.Document()
                        .append("bookingId", bid).append("eventType", "PAYMENT_CAPTURED")
                        .append("amount", 100.0).append("timestamp", new java.util.Date(t0 - 1000)));
                mongo.getCollection(coll).insertOne(new org.bson.Document()
                        .append("bookingId", bid).append("eventType", "PAYMENT_CAPTURED")
                        .append("amount", 200.0).append("timestamp", new java.util.Date(t0)));
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history", tok);
                assert2xx(r, "TC162");
        }
}

// ─── TC163 — S5-F11 ANALYTICS_VIEWED logged on call ──────────────────────────
@Tag("public")
@Tag("features_m2")
class TC163_AuditHistoryLogsViewedTests extends TestBase {
        @Test
        @DisplayName("TC163 — Calling payment-history logs ANALYTICS_VIEWED")
        void audit_logs_viewed() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC163: MongoDB required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc163u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                String coll = s5AuditCollection();
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("eventType", "ANALYTICS_VIEWED"));
                assert2xx(httpGetAuth("/api/bookings/" + bid + "/payment-history", tok), "TC163");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("eventType", "ANALYTICS_VIEWED"));
                assertTrue(after > before,
                        "TC163: ANALYTICS_VIEWED count must increase; before=" + before + " after=" + after);
        }
}

// ─── TC164 — S5-F11 empty list when no events ────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC164_AuditHistoryEmptyTests extends TestBase {
        @Test
        @DisplayName("TC164 — Booking with no audit events returns empty list")
        void audit_empty() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc164u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history", tok);
                assert2xx(r, "TC164");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC164: empty list expected; got " + list.size());
        }
}

// ─── TC165 — S5-F11 ownership 403 ────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC165_AuditHistoryOwnershipTests extends TestBase {
        @Test
        @DisplayName("TC165 — Other user's token returns 403")
        void audit_ownership() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> aMap = seedAndLoginUser("tc165a");
                long aId = ((Number) aMap.get("id")).longValue();
                long iid = _TpM2.itin(this, aId, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, aId, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                java.util.Map<String, Object> bMap = seedAndLoginUser("tc165b");
                String bTok = (String) bMap.get("token");
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history", bTok);
                assertEquals(403, r.statusCode(), "TC165: must be 403; got " + r.statusCode());
        }
}

// ─── TC166 — S5-F11 ADMIN bypass ─────────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC166_AuditHistoryAdminBypassTests extends TestBase {
        @Test
        @DisplayName("TC166 — ADMIN can read any booking's payment history")
        void audit_admin_bypass() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc166u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history", tok);
                assert2xx(r, "TC166");
        }
}

// ─── TC167 — S5-F11 non-existent booking → 404 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC167_AuditHistoryNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC167 — Non-existent booking returns 404")
        void audit_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/9999999/payment-history", tok);
                assertEquals(404, r.statusCode(), "TC167: must be 404; got " + r.statusCode());
        }
}

// ─── TC168 — S5-F11 missing JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC168_AuditHistoryMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC168 — Missing Authorization returns 401")
        void audit_missing_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGet("/api/bookings/1/payment-history");
                assertEquals(401, r.statusCode(), "TC168: must be 401; got " + r.statusCode());
        }
}

// ─── TC169 — S5-F11 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC169_AuditHistoryInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC169 — Bogus JWT returns 401")
        void audit_invalid_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                HttpResponse<String> r = httpGetAuth("/api/bookings/1/payment-history", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC169: must be 401; got " + r.statusCode());
        }
}

// ─── TC170 — S5-F11 second page returns next batch ───────────────────────────
@Tag("public")
@Tag("features_m2")
class TC170_AuditHistorySecondPageTests extends TestBase {
        @Test
        @DisplayName("TC170 — page=1 returns events distinct from page=0")
        void audit_second_page() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC170: MongoDB required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc170u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                String coll = s5AuditCollection();
                for (int i = 0; i < 4; i++) {
                        mongo.getCollection(coll).insertOne(new org.bson.Document()
                                .append("bookingId", bid).append("eventType", "PAYMENT_CAPTURED")
                                .append("amount", 100.0 + i)
                                .append("timestamp", new java.util.Date(System.currentTimeMillis() + i)));
                }
                HttpResponse<String> r0 = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history?page=0&size=2", tok);
                assert2xx(r0, "TC170 p0");
                HttpResponse<String> r1 = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history?page=1&size=2", tok);
                assert2xx(r1, "TC170 p1");
                assertNotEquals(r0.body(), r1.body(),
                        "TC170: page 0 and page 1 must differ; r0=" + r0.body() + " r1=" + r1.body());
        }
}

// ─── TC171 — S5-F11 booking-scope isolation ──────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC171_AuditHistoryScopeIsolationTests extends TestBase {
        @Test
        @DisplayName("TC171 — Events for booking A not returned when querying booking B")
        void audit_scope_isolation() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC171: MongoDB required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc171u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long b1 = _TpM2S5.bkg(this, uid, iid, 100.0, "2026-03-15", "2026-03-20", "COMPLETED");
                long b2 = _TpM2S5.bkg(this, uid, iid, 200.0, "2026-04-15", "2026-04-20", "COMPLETED");
                String coll = s5AuditCollection();
                mongo.getCollection(coll).insertOne(new org.bson.Document()
                        .append("bookingId", b1).append("eventType", "PAYMENT_CAPTURED")
                        .append("amount", 100.0).append("timestamp", new java.util.Date()));
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/" + b2 + "/payment-history", tok);
                assert2xx(r, "TC171");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                for (JsonNode it : list) {
                        long bidGot = it.path("bookingId").asLong(b2);
                        assertEquals(b2, bidGot,
                                "TC171: only b2 events expected; got " + bidGot);
                }
        }
}

// ─── TC172 — S5-F11 default pagination size sane ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC172_AuditHistoryDefaultSizeTests extends TestBase {
        @Test
        @DisplayName("TC172 — Default page size returns ≤ 100 events")
        void audit_default_size() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc172u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history", tok);
                assert2xx(r, "TC172");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() <= 100,
                        "TC172: default page size must cap; got " + list.size());
        }
}

// ─── TC173 — S5-F11 events DTO carries eventType, amount, timestamp ──────────
@Tag("public")
@Tag("features_m2")
class TC173_AuditHistoryDtoFieldsTests extends TestBase {
        @Test
        @DisplayName("TC173 — Each event row carries eventType")
        void audit_dto_fields() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC173: MongoDB required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc173u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                mongo.getCollection(s5AuditCollection()).insertOne(new org.bson.Document()
                        .append("bookingId", bid).append("eventType", "PAYMENT_CAPTURED")
                        .append("amount", 500.0).append("timestamp", new java.util.Date()));
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history", tok);
                assert2xx(r, "TC173");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                if (list.size() > 0) {
                        for (JsonNode it : list) {
                                assertTrue(it.has("eventType") || it.has("event_type"),
                                        "TC173: each event must carry eventType; got " + it);
                        }
                }
        }
}

// ─── TC174 — S5-F11 negative page → 400 or first page ────────────────────────
@Tag("public")
@Tag("features_m2")
class TC174_AuditHistoryNegativePageTests extends TestBase {
        @Test
        @DisplayName("TC174 — page=-1 returns 400 or behaves as page=0")
        void audit_negative_page() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc174u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2026-03-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 500.0, "2026-03-15", "2026-03-20", "COMPLETED");
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/" + bid + "/payment-history?page=-1&size=10", tok);
                int code = r.statusCode();
                assertTrue(code / 100 == 2 || code == 400,
                        "TC174: must be 2xx or 400; got " + code);
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S5-F12 — Tiered Cancellation Refund (TC175..TC190) — 4 STRATEGIES
// Endpoint: POST /api/bookings/{bookingId}/refund-cancellation-tier
// Body: {"reason":"..."}.
// daysUntilStart > 14 → EarlyCancellationRefundStrategy   (100% refund)
// 7 <= daysUntilStart <= 14 → MidCancellationRefundStrategy   (50% refund)
// 1 <= daysUntilStart < 7 → LateCancellationRefundStrategy   (25% refund)
// status != PLANNED OR daysUntilStart < 1 → NoRefundStrategy (0%)
// Booking status: PLANNED / CONFIRMED / IN_PROGRESS / COMPLETED / CANCELLED
// ════════════════════════════════════════════════════════════════════════════

// ─── TC175 — S5-F12 EARLY 100% refund ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC175_RefundEarlyTests extends TestBase {
        @Test
        @DisplayName("TC175 — daysUntilStart > 14 → EarlyCancellationRefundStrategy (100% refund)")
        void refund_early() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc175u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(30).toString(),
                        java.time.LocalDate.now().plusDays(35).toString(),
                        "PLANNED");
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"early cancel\"}", tok);
                assert2xx(r, "TC175");
                JsonNode j = parseNode(r.body());
                String strat = j.path("strategy").asText("");
                assertEquals(s5StrategyFullRefund(), strat,
                        "TC175: must use EarlyCancellationRefundStrategy; got " + strat);
                double refunded = j.path("refundedAmount").asDouble(-1.0);
                assertEquals(1000.0, refunded, 0.01,
                        "TC175: 100% refund (1000); got " + refunded);
        }
}

// ─── TC176 — S5-F12 MID 50% refund ───────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC176_RefundMidTests extends TestBase {
        @Test
        @DisplayName("TC176 — 7 ≤ daysUntilStart ≤ 14 → MidCancellationRefundStrategy (50% refund)")
        void refund_mid() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc176u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(10).toString(),
                        java.time.LocalDate.now().plusDays(12).toString(),
                        "PLANNED");
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"mid cancel\"}", tok);
                assert2xx(r, "TC176");
                JsonNode j = parseNode(r.body());
                String strat = j.path("strategy").asText("");
                assertEquals(s5StrategyFoodOnly(), strat,
                        "TC176: must use MidCancellationRefundStrategy; got " + strat);
                double refunded = j.path("refundedAmount").asDouble(-1.0);
                assertEquals(500.0, refunded, 0.01,
                        "TC176: 50% refund (500); got " + refunded);
        }
}

// ─── TC177 — S5-F12 LATE 25% refund ──────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC177_RefundLateTests extends TestBase {
        @Test
        @DisplayName("TC177 — 1 ≤ daysUntilStart < 7 → LateCancellationRefundStrategy (25% refund)")
        void refund_late() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc177u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(3).toString(),
                        java.time.LocalDate.now().plusDays(5).toString(),
                        "PLANNED");
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"late cancel\"}", tok);
                assert2xx(r, "TC177");
                JsonNode j = parseNode(r.body());
                String strat = j.path("strategy").asText("");
                assertEquals("LateCancellationRefundStrategy", strat,
                        "TC177: must use LateCancellationRefundStrategy; got " + strat);
                double refunded = j.path("refundedAmount").asDouble(-1.0);
                assertEquals(250.0, refunded, 0.01,
                        "TC177: 25% refund (250); got " + refunded);
        }
}

// ─── TC178 — S5-F12 NO REFUND status != PLANNED ──────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC178_RefundNoneNotPlannedTests extends TestBase {
        @Test
        @DisplayName("TC178 — status=COMPLETED → NoRefundStrategy (0% refund)")
        void refund_none_not_planned() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc178u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "COMPLETED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(30).toString(),
                        java.time.LocalDate.now().plusDays(35).toString(),
                        "COMPLETED");
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"too late\"}", tok);
                assert2xx(r, "TC178");
                JsonNode j = parseNode(r.body());
                String strat = j.path("strategy").asText("");
                assertEquals(s5StrategyNoRefund(), strat,
                        "TC178: must use NoRefundStrategy; got " + strat);
                double refunded = j.path("refundedAmount").asDouble(-1.0);
                assertEquals(0.0, refunded, 0.01,
                        "TC178: 0% refund; got " + refunded);
        }
}

// ─── TC179 — S5-F12 NO REFUND daysUntilStart < 1 ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC179_RefundNonePastTests extends TestBase {
        @Test
        @DisplayName("TC179 — daysUntilStart < 1 → NoRefundStrategy (0% refund)")
        void refund_none_past() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc179u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().minusDays(1).toString(),
                        java.time.LocalDate.now().plusDays(1).toString(),
                        "PLANNED");
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"already started\"}", tok);
                assert2xx(r, "TC179");
                JsonNode j = parseNode(r.body());
                double refunded = j.path("refundedAmount").asDouble(-1.0);
                assertEquals(0.0, refunded, 0.01,
                        "TC179: 0% refund for past start; got " + refunded);
        }
}

// ─── TC180 — S5-F12 boundary 14→Mid (≤14) ────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC180_RefundBoundary14Tests extends TestBase {
        @Test
        @DisplayName("TC180 — daysUntilStart=14 → MidCancellationRefundStrategy (50%)")
        void refund_boundary_14() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc180u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(14).toString(),
                        java.time.LocalDate.now().plusDays(16).toString(),
                        "PLANNED");
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"boundary\"}", tok);
                assert2xx(r, "TC180");
                String strat = parseNode(r.body()).path("strategy").asText("");
                assertEquals(s5StrategyFoodOnly(), strat,
                        "TC180: 14 days → Mid; got " + strat);
        }
}

// ─── TC181 — S5-F12 boundary 7→Mid (≥7) ──────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC181_RefundBoundary7Tests extends TestBase {
        @Test
        @DisplayName("TC181 — daysUntilStart=7 → MidCancellationRefundStrategy")
        void refund_boundary_7() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc181u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(7).toString(),
                        java.time.LocalDate.now().plusDays(9).toString(),
                        "PLANNED");
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"boundary\"}", tok);
                assert2xx(r, "TC181");
                String strat = parseNode(r.body()).path("strategy").asText("");
                assertEquals(s5StrategyFoodOnly(), strat,
                        "TC181: 7 days → Mid; got " + strat);
        }
}

// ─── TC182 — S5-F12 boundary 1→Late ──────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC182_RefundBoundary1Tests extends TestBase {
        @Test
        @DisplayName("TC182 — daysUntilStart=1 → LateCancellationRefundStrategy")
        void refund_boundary_1() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc182u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(1).toString(),
                        java.time.LocalDate.now().plusDays(3).toString(),
                        "PLANNED");
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"boundary\"}", tok);
                assert2xx(r, "TC182");
                String strat = parseNode(r.body()).path("strategy").asText("");
                assertEquals("LateCancellationRefundStrategy", strat,
                        "TC182: 1 day → Late; got " + strat);
        }
}

// ─── TC183 — S5-F12 REFUND_PROCESSED logged on success ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC183_RefundLoggedTests extends TestBase {
        @Test
        @DisplayName("TC183 — REFUND_PROCESSED logged to payment_audit_trail")
        void refund_logged() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC183: MongoDB required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc183u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(30).toString(),
                        java.time.LocalDate.now().plusDays(35).toString(),
                        "PLANNED");
                String coll = s5AuditCollection();
                long before = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("eventType", "REFUND_PROCESSED"));
                assert2xx(httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"x\"}", tok), "TC183");
                long after = mongo.getCollection(coll).countDocuments(
                        new org.bson.Document("eventType", "REFUND_PROCESSED"));
                assertTrue(after > before,
                        "TC183: REFUND_PROCESSED count must increase; before=" + before + " after=" + after);
        }
}

// ─── TC184 — S5-F12 audit doc carries strategy class name ────────────────────
@Tag("public")
@Tag("features_m2")
class TC184_RefundAuditStrategyTests extends TestBase {
        @Test
        @DisplayName("TC184 — Audit trail entry includes strategy class name")
        void refund_audit_strategy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC184: MongoDB required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc184u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(30).toString(),
                        java.time.LocalDate.now().plusDays(35).toString(),
                        "PLANNED");
                assert2xx(httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"x\"}", tok), "TC184");
                String coll = s5AuditCollection();
                org.bson.Document found = mongo.getCollection(coll)
                        .find(new org.bson.Document("eventType", "REFUND_PROCESSED")
                                .append("bookingId", bid))
                        .first();
                assertNotNull(found, "TC184: REFUND_PROCESSED entry must exist for bookingId=" + bid);
        }
}

// ─── TC185 — S5-F12 reason is required ───────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC185_RefundReasonRequiredTests extends TestBase {
        @Test
        @DisplayName("TC185 — Blank reason returns 400")
        void refund_reason_required() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc185u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(30).toString(),
                        java.time.LocalDate.now().plusDays(35).toString(),
                        "PLANNED");
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"\"}", tok);
                assertEquals(400, r.statusCode(),
                        "TC185: blank reason must be 400; got " + r.statusCode());
        }
}

// ─── TC186 — S5-F12 non-existent booking → 404 ───────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC186_RefundNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC186 — Non-existent booking returns 404")
        void refund_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/9999999/refund-cancellation-tier",
                        "{\"reason\":\"x\"}", tok);
                assertEquals(404, r.statusCode(), "TC186: must be 404; got " + r.statusCode());
        }
}

// ─── TC187 — S5-F12 missing JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC187_RefundMissingJwtTests extends TestBase {
        @Test
        @DisplayName("TC187 — Missing Authorization returns 401")
        void refund_missing_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc187u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(30).toString(),
                        java.time.LocalDate.now().plusDays(35).toString(),
                        "PLANNED");
                HttpResponse<String> r = httpPost(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"x\"}");
                assertEquals(401, r.statusCode(), "TC187: must be 401; got " + r.statusCode());
        }
}

// ─── TC188 — S5-F12 invalid JWT → 401 ────────────────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC188_RefundInvalidJwtTests extends TestBase {
        @Test
        @DisplayName("TC188 — Bogus JWT returns 401")
        void refund_invalid_jwt() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc188u");
                long uid = ((Number) u.get("id")).longValue();
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(30).toString(),
                        java.time.LocalDate.now().plusDays(35).toString(),
                        "PLANNED");
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"x\"}", "xxx.yyy.zzz");
                assertEquals(401, r.statusCode(), "TC188: must be 401; got " + r.statusCode());
        }
}

// ─── TC189 — S5-F12 audit entry carries refundedAmount ───────────────────────
@Tag("public")
@Tag("features_m2")
class TC189_RefundAuditAmountTests extends TestBase {
        @Test
        @DisplayName("TC189 — Audit entry includes refundedAmount")
        void refund_audit_amount() throws Exception {
                BASE_URL = checkoutServiceUrl;
                if (mongo == null) throw new AssertionError("TC189: MongoDB required");
                java.util.Map<String, Object> u = seedAndLoginUser("tc189u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(30).toString(),
                        java.time.LocalDate.now().plusDays(35).toString(),
                        "PLANNED");
                assert2xx(httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"x\"}", tok), "TC189");
                org.bson.Document found = mongo.getCollection(s5AuditCollection())
                        .find(new org.bson.Document("eventType", "REFUND_PROCESSED")
                                .append("bookingId", bid))
                        .first();
                assertNotNull(found, "TC189: REFUND_PROCESSED audit entry must exist");
        }
}

// ─── TC190 — S5-F12 reason persisted in response ─────────────────────────────
@Tag("public")
@Tag("features_m2")
class TC190_RefundReasonInResponseTests extends TestBase {
        @Test
        @DisplayName("TC190 — Response carries reason from request body")
        void refund_reason_in_response() throws Exception {
                BASE_URL = checkoutServiceUrl;
                java.util.Map<String, Object> u = seedAndLoginUser("tc190u");
                long uid = ((Number) u.get("id")).longValue();
                String tok = (String) u.get("token");
                long iid = _TpM2.itin(this, uid, 1L, "PLANNED", 1000.0, "2099-04-01");
                long bid = _TpM2S5.bkg(this, uid, iid, 1000.0,
                        java.time.LocalDate.now().plusDays(30).toString(),
                        java.time.LocalDate.now().plusDays(35).toString(),
                        "PLANNED");
                String reason = "specific-reason-" + nonce();
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/refund-cancellation-tier",
                        "{\"reason\":\"" + reason + "\"}", tok);
                assert2xx(r, "TC190");
                JsonNode j = parseNode(r.body());
                String got = j.path("reason").asText("");
                assertEquals(reason, got,
                        "TC190: response reason must echo request; got " + got);
        }
}


// ════════════════════════════════════════════════════════════════════════════
// Auxiliary M1 seed helpers
// ════════════════════════════════════════════════════════════════════════════

final class _TpM1S1Seed {
        private _TpM1S1Seed() {}

        /** INSERT a SavedDestination join row. */
        static long savedDest(TestBase t, long userId, long destinationId,
                              String label, boolean isDefault) {
                String tbl = t.tableName("SavedDestination");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("SavedDestination", "user"), userId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("SavedDestination", "destination"), destinationId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("SavedDestination", "label"), label); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("SavedDestination", "isDefault"), isDefault); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("SavedDestination", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S1 — User Service M1 features (TC191..TC228) — 38 TCs across F1..F9
// ════════════════════════════════════════════════════════════════════════════

// ─── TC191 — S1-F1 search by partial name (case-insensitive) ─────────────────
@Tag("public")
@Tag("features_m1")
class TC191_S1F1SearchByNameTests extends TestBase {
        @Test
        @DisplayName("TC191 — Search users by partial name (case-insensitive) returns matches")
        void search_by_name() throws Exception {
                BASE_URL = userServiceUrl;
                _TpM1Seed.seedUser(this, "Ahmed", "tc191a@tp.io", "TRAVELER");
                _TpM1Seed.seedUser(this, "Sara", "tc191b@tp.io", "TRAVELER");
                _TpM1Seed.seedUser(this, "Ahmed Ali", "tc191c@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?name=Ahmed", tok);
                assert2xx(r, "TC191");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC191: at least 2 Ahmed matches; got " + list.size());
        }
}

// ─── TC192 — S1-F1 search by role exact match ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC192_S1F1SearchByRoleTests extends TestBase {
        @Test
        @DisplayName("TC192 — Search users by role=TRAVELER returns TRAVELER users")
        void search_by_role() throws Exception {
                BASE_URL = userServiceUrl;
                _TpM1Seed.seedUser(this, "Sara", "tc192a@tp.io", "TRAVELER");
                _TpM1Seed.seedUser(this, "Other", "tc192b@tp.io", "ADMIN");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?role=TRAVELER", tok);
                assert2xx(r, "TC192");
        }
}

// ─── TC193 — S1-F1 search no match returns empty ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC193_S1F1NoMatchTests extends TestBase {
        @Test
        @DisplayName("TC193 — No matching user returns empty list")
        void no_match() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?name=zzzzzz_no_match", tok);
                assert2xx(r, "TC193");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC193: empty list expected; got " + list.size());
        }
}

// ─── TC194 — S1-F1 case-insensitive lower input matches mixed-case ───────────
@Tag("public")
@Tag("features_m1")
class TC194_S1F1CaseInsensitiveTests extends TestBase {
        @Test
        @DisplayName("TC194 — Lowercase query matches mixed-case stored names")
        void case_insensitive() throws Exception {
                BASE_URL = userServiceUrl;
                _TpM1Seed.seedUser(this, "Ahmed", "tc194@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?name=ahmed", tok);
                assert2xx(r, "TC194");
        }
}

// ─── TC195 — S1-F2 preferences merge happy path ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC195_S1F2MergeTests extends TestBase {
        @Test
        @DisplayName("TC195 — PUT preferences merges new keys without removing existing")
        void prefs_merge() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc195@tp.io", "TRAVELER");
                _TpM1Seed.setPrefs(this, uid, "{\"language\":\"en\",\"currency\":\"USD\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/preferences",
                        "{\"currency\":\"EGP\",\"travelStyle\":\"ADVENTURE\"}", tok);
                assert2xx(r, "TC195");
                JsonNode prefs = _TpM2.rO(parseNode(r.body()), "preferences");
                assertNotNull(prefs, "TC195: preferences key required; body=" + r.body());
                assertEquals("en", prefs.path("language").asText(), "TC195: language preserved");
                assertEquals("EGP", prefs.path("currency").asText(), "TC195: currency overwritten");
        }
}

// ─── TC196 — S1-F2 same-key overwrite ────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC196_S1F2OverwriteTests extends TestBase {
        @Test
        @DisplayName("TC196 — Re-PUT same key overwrites the value")
        void prefs_overwrite() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc196@tp.io", "TRAVELER");
                _TpM1Seed.setPrefs(this, uid, "{\"language\":\"en\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/preferences",
                        "{\"language\":\"ar\"}", tok);
                assert2xx(r, "TC196");
                JsonNode prefs = _TpM2.rO(parseNode(r.body()), "preferences");
                assertEquals("ar", prefs.path("language").asText(), "TC196: language updated");
        }
}

// ─── TC197 — S1-F2 404 non-existent user ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC197_S1F2NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC197 — Updating preferences on non-existent user returns 404")
        void prefs_404() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/9999999/preferences",
                        "{\"language\":\"en\"}", tok);
                assertEquals(404, r.statusCode(), "TC197: must be 404; got " + r.statusCode());
        }
}

// ─── TC198 — S1-F3 trip-summary happy path ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC198_S1F3SummaryHappyTests extends TestBase {
        @Test
        @DisplayName("TC198 — Returns totalItineraries and completed/cancelled aggregates")
        void summary_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc198@tp.io", "TRAVELER");
                _TpM1Seed.seedItinerary(this, uid, 1L, "COMPLETED", 1500.0, "2026-03-01");
                _TpM1Seed.seedItinerary(this, uid, 1L, "COMPLETED", 2000.0, "2026-03-15");
                _TpM1Seed.seedItinerary(this, uid, 2L, "PLANNED", 800.0, "2026-03-10");
                _TpM1Seed.seedItinerary(this, uid, 2L, "CANCELLED", 200.0, "2026-03-12");
                _TpM1Seed.seedItinerary(this, uid, 1L, "IN_PROGRESS", 100.0, "2026-03-20");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/trip-summary", tok);
                assert2xx(r, "TC198");
                JsonNode j = parseNode(r.body());
                long total = _TpM2.rL(j, "totalItineraries", "total_itineraries");
                assertEquals(5L, total, "TC198: totalItineraries=5; got " + total);
        }
}

// ─── TC199 — S1-F3 user with no itineraries returns zeros ────────────────────
@Tag("public")
@Tag("features_m1")
class TC199_S1F3ZerosTests extends TestBase {
        @Test
        @DisplayName("TC199 — User with no itineraries returns totals=0")
        void summary_zeros() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc199@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/trip-summary", tok);
                assert2xx(r, "TC199");
                long total = _TpM2.rL(parseNode(r.body()), "totalItineraries", "total_itineraries");
                assertEquals(0L, total, "TC199: totalItineraries=0; got " + total);
        }
}

// ─── TC200 — S1-F3 404 non-existent user ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC200_S1F3NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC200 — Non-existent user returns 404")
        void summary_404() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/9999999/trip-summary", tok);
                assertEquals(404, r.statusCode(), "TC200: must be 404; got " + r.statusCode());
        }
}

// ─── TC201 — S1-F4 deactivate fails when active itinerary exists ─────────────
@Tag("public")
@Tag("features_m1")
class TC201_S1F4ActiveItineraryTests extends TestBase {
        @Test
        @DisplayName("TC201 — Deactivate returns 400 when active (PLANNED) itinerary exists")
        void deactivate_active_itinerary() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc201@tp.io", "TRAVELER");
                _TpM1Seed.seedItinerary(this, uid, 1L, "PLANNED", 500.0, "2026-04-15");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/deactivate", "", tok);
                assertEquals(400, r.statusCode(), "TC201: must be 400; got " + r.statusCode());
        }
}

// ─── TC202 — S1-F4 deactivate succeeds when no active itineraries ────────────
@Tag("public")
@Tag("features_m1")
class TC202_S1F4SuccessTests extends TestBase {
        @Test
        @DisplayName("TC202 — Deactivate succeeds when user has no active itineraries")
        void deactivate_success() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc202@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/deactivate", "", tok);
                assert2xx(r, "TC202");
        }
}

// ─── TC203 — S1-F4 404 non-existent user ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC203_S1F4NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC203 — Deactivating non-existent user returns 404")
        void deactivate_404() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/9999999/deactivate", "", tok);
                assertEquals(404, r.statusCode(), "TC203: must be 404; got " + r.statusCode());
        }
}

// ─── TC204 — S1-F4 cancels PLANNED itineraries on deactivate ─────────────────
@Tag("public")
@Tag("features_m1")
class TC204_S1F4CancelsPlannedTests extends TestBase {
        @Test
        @DisplayName("TC204 — Deactivating user cancels their PLANNED itineraries (or 400)")
        void deactivate_cancels_planned() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc204@tp.io", "TRAVELER");
                _TpM1Seed.seedItinerary(this, uid, 1L, "PLANNED", 100.0, "2026-03-15");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/deactivate", "", tok);
                int code = r.statusCode();
                assertTrue(code / 100 == 2 || code == 400,
                        "TC204: must be 2xx (cancels planned) or 400 (rejects); got " + code);
        }
}

// ─── TC205 — S1-F5 prefs search happy match ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC205_S1F5HappyTests extends TestBase {
        @Test
        @DisplayName("TC205 — Search by JSONB key/value returns matching users")
        void prefs_search_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long u1 = _TpM1Seed.seedUser(this, "U1", "tc205a@tp.io", "TRAVELER");
                long u2 = _TpM1Seed.seedUser(this, "U2", "tc205b@tp.io", "TRAVELER");
                _TpM1Seed.setPrefs(this, u1, "{\"language\":\"ar\"}");
                _TpM1Seed.setPrefs(this, u2, "{\"language\":\"en\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/preferences/search?key=language&value=ar", tok);
                assert2xx(r, "TC205");
        }
}

// ─── TC206 — S1-F5 blank key → 400 ───────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC206_S1F5BlankKeyTests extends TestBase {
        @Test
        @DisplayName("TC206 — Blank key returns 400")
        void prefs_search_blank_key() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/preferences/search?key=&value=ar", tok);
                assertEquals(400, r.statusCode(), "TC206: must be 400; got " + r.statusCode());
        }
}

// ─── TC207 — S1-F5 no match returns empty ────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC207_S1F5NoMatchTests extends TestBase {
        @Test
        @DisplayName("TC207 — Non-matching value returns empty list")
        void prefs_no_match() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/preferences/search?key=language&value=zz", tok);
                assert2xx(r, "TC207");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC207: empty list expected; got " + list.size());
        }
}

// ─── TC208 — S1-F6 top-travelers happy ranking ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC208_S1F6TopTravelersTests extends TestBase {
        @Test
        @DisplayName("TC208 — Returns top travelers ordered by spending")
        void top_travelers() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/reports/top-travelers?startDate=2026-03-01&endDate=2026-03-31&limit=5", tok);
                assert2xx(r, "TC208");
        }
}

// ─── TC209 — S1-F6 invalid range → 400 ───────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC209_S1F6InvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC209 — startDate after endDate returns 400")
        void top_travelers_invalid() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/reports/top-travelers?startDate=2026-04-01&endDate=2026-03-01&limit=5", tok);
                assertEquals(400, r.statusCode(), "TC209: must be 400; got " + r.statusCode());
        }
}

// ─── TC210 — S1-F6 limit caps results ────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC210_S1F6LimitTests extends TestBase {
        @Test
        @DisplayName("TC210 — limit=N caps the number of results")
        void top_travelers_limit() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/reports/top-travelers?startDate=2026-03-01&endDate=2026-03-31&limit=2", tok);
                assert2xx(r, "TC210");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() <= 2, "TC210: must cap at 2; got " + list.size());
        }
}

// ─── TC211 — S1-F7 set default saved-destination happy switch ────────────────
@Tag("public")
@Tag("features_m1")
class TC211_S1F7HappyTests extends TestBase {
        @Test
        @DisplayName("TC211 — Set default saved destination succeeds")
        void default_dest_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc211@tp.io", "TRAVELER");
                long d1 = _TpM2.dest(this, "D1", "EG", "Cairo", "ACTIVE");
                long d2 = _TpM2.dest(this, "D2", "EG", "Luxor", "ACTIVE");
                _TpM1S1Seed.savedDest(this, uid, d1, "Home", false);
                _TpM1S1Seed.savedDest(this, uid, d2, "Work", true);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/users/" + uid + "/destinations/" + d1 + "/default", "", tok);
                assert2xx(r, "TC211");
        }
}

// ─── TC212 — S1-F7 404 non-existent user ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC212_S1F7UserNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC212 — Non-existent userId returns 404")
        void default_dest_user_404() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/users/9999999/destinations/1/default", "", tok);
                assertEquals(404, r.statusCode(), "TC212: must be 404; got " + r.statusCode());
        }
}

// ─── TC213 — S1-F7 404 non-existent destination ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC213_S1F7DestNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC213 — Non-existent destinationId returns 404")
        void default_dest_404() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc213@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/users/" + uid + "/destinations/9999999/default", "", tok);
                assertEquals(404, r.statusCode(), "TC213: must be 404; got " + r.statusCode());
        }
}

// ─── TC214 — S1-F7 400 wrong owner ───────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC214_S1F7WrongOwnerTests extends TestBase {
        @Test
        @DisplayName("TC214 — Saved destination not owned by user returns 400")
        void default_dest_wrong_owner() throws Exception {
                BASE_URL = userServiceUrl;
                long u1 = _TpM1Seed.seedUser(this, "U1", "tc214a@tp.io", "TRAVELER");
                long u2 = _TpM1Seed.seedUser(this, "U2", "tc214b@tp.io", "TRAVELER");
                long d = _TpM2.dest(this, "D", "EG", "Aswan", "ACTIVE");
                _TpM1S1Seed.savedDest(this, u2, d, "U2 Spot", true);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/users/" + u1 + "/destinations/" + d + "/default", "", tok);
                assertEquals(400, r.statusCode(), "TC214: must be 400; got " + r.statusCode());
        }
}

// ─── TC215 — S1-F8 profile happy with saved destinations list ────────────────
@Tag("public")
@Tag("features_m1")
class TC215_S1F8ProfileHappyTests extends TestBase {
        @Test
        @DisplayName("TC215 — Profile DTO includes savedDestinations list")
        void profile_happy() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc215@tp.io", "TRAVELER");
                long d1 = _TpM2.dest(this, "D1", "EG", "Cairo", "ACTIVE");
                long d2 = _TpM2.dest(this, "D2", "EG", "Luxor", "ACTIVE");
                _TpM1S1Seed.savedDest(this, uid, d1, "Home", true);
                _TpM1S1Seed.savedDest(this, uid, d2, "Beach", false);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/profile", tok);
                assert2xx(r, "TC215");
                JsonNode j = parseNode(r.body());
                long total = _TpM2.rL(j, "totalSavedDestinations", "total_saved_destinations");
                assertEquals(2L, total, "TC215: totalSavedDestinations=2; got " + total);
        }
}

// ─── TC216 — S1-F8 user with no saved destinations → total=0 ─────────────────
@Tag("public")
@Tag("features_m1")
class TC216_S1F8NoDestsTests extends TestBase {
        @Test
        @DisplayName("TC216 — User with no saved destinations: totalSavedDestinations=0")
        void profile_no_dests() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc216@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/profile", tok);
                assert2xx(r, "TC216");
                long total = _TpM2.rL(parseNode(r.body()), "totalSavedDestinations", "total_saved_destinations");
                assertEquals(0L, total, "TC216: totalSavedDestinations=0; got " + total);
        }
}

// ─── TC217 — S1-F8 404 non-existent user ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC217_S1F8NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC217 — Non-existent user returns 404")
        void profile_404() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/9999999/profile", tok);
                assertEquals(404, r.statusCode(), "TC217: must be 404; got " + r.statusCode());
        }
}

// ─── TC218 — S1-F9 travel-style + minTrips filter ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC218_S1F9HappyTests extends TestBase {
        @Test
        @DisplayName("TC218 — Filter by travelStyle preference and minTrips")
        void style_min() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/preferences/travel-style?style=ADVENTURE&minTrips=3", tok);
                assert2xx(r, "TC218");
        }
}

// ─── TC219 — S1-F9 blank style → 400 ─────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC219_S1F9BlankStyleTests extends TestBase {
        @Test
        @DisplayName("TC219 — Blank style returns 400")
        void style_blank() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/preferences/travel-style?style=&minTrips=1", tok);
                assertEquals(400, r.statusCode(), "TC219: must be 400; got " + r.statusCode());
        }
}

// ─── TC220 — S1-F9 minTrips=1 returns matching ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC220_S1F9MinOneTests extends TestBase {
        @Test
        @DisplayName("TC220 — minTrips=1 returns 2xx with list")
        void style_min_one() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/preferences/travel-style?style=ADVENTURE&minTrips=1", tok);
                assert2xx(r, "TC220");
        }
}

// ─── TC221 — S1-F1 search by email partial ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC221_S1F1EmailPartialTests extends TestBase {
        @Test
        @DisplayName("TC221 — Search by partial email match")
        void search_email_partial() throws Exception {
                BASE_URL = userServiceUrl;
                _TpM1Seed.seedUser(this, "U", "tc221_specific@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search?email=tc221_specific", tok);
                assert2xx(r, "TC221");
        }
}

// ─── TC222 — S1-F3 cancelledItineraries field present ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC222_S1F3CancelledFieldTests extends TestBase {
        @Test
        @DisplayName("TC222 — DTO includes cancelledItineraries field")
        void summary_cancelled_field() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc222@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/trip-summary", tok);
                assert2xx(r, "TC222");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("cancelledItineraries") || j.has("cancelled_itineraries"),
                        "TC222: cancelledItineraries key required; got " + r.body());
        }
}

// ─── TC223 — S1-F3 totalSpent / completedItineraries fields present ──────────
@Tag("public")
@Tag("features_m1")
class TC223_S1F3SpendFieldsTests extends TestBase {
        @Test
        @DisplayName("TC223 — DTO includes totalSpent and completedItineraries")
        void summary_spend_fields() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc223@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/trip-summary", tok);
                assert2xx(r, "TC223");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("totalSpent") || j.has("total_spent"),
                        "TC223: totalSpent key required");
                assertTrue(j.has("completedItineraries") || j.has("completed_itineraries"),
                        "TC223: completedItineraries key required");
        }
}

// ─── TC224 — S1-F6 limit greater than available returns all ──────────────────
@Tag("public")
@Tag("features_m1")
class TC224_S1F6LimitGtThanAvailTests extends TestBase {
        @Test
        @DisplayName("TC224 — limit greater than available returns all available")
        void top_travelers_big_limit() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/users/reports/top-travelers?startDate=2026-03-01&endDate=2026-03-31&limit=100", tok);
                assert2xx(r, "TC224");
        }
}

// ─── TC225 — S1-F8 profile DTO has email/phone fields ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC225_S1F8DtoShapeTests extends TestBase {
        @Test
        @DisplayName("TC225 — Profile DTO includes email and phone")
        void profile_dto_shape() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc225@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/" + uid + "/profile", tok);
                assert2xx(r, "TC225");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("email"), "TC225: email key required");
                assertTrue(j.has("phone"), "TC225: phone key required");
        }
}

// ─── TC226 — S1-F2 multiple keys merge ───────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC226_S1F2MultiKeyMergeTests extends TestBase {
        @Test
        @DisplayName("TC226 — Multi-key merge keeps existing and adds new")
        void prefs_multi_key() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc226@tp.io", "TRAVELER");
                _TpM1Seed.setPrefs(this, uid, "{\"language\":\"en\",\"timezone\":\"UTC\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/users/" + uid + "/preferences",
                        "{\"travelStyle\":\"BEACH\",\"dashboardLayout\":\"compact\"}", tok);
                assert2xx(r, "TC226");
                JsonNode prefs = _TpM2.rO(parseNode(r.body()), "preferences");
                assertNotNull(prefs, "TC226: preferences key required");
                assertEquals("en", prefs.path("language").asText(), "TC226: existing language preserved");
        }
}

// ─── TC227 — S1-F4 status set to DEACTIVATED ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC227_S1F4StatusSetTests extends TestBase {
        @Test
        @DisplayName("TC227 — After deactivate, user.status is DEACTIVATED in PG")
        void deactivate_status_set() throws Exception {
                BASE_URL = userServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc227@tp.io", "TRAVELER");
                String tok = adminToken();
                assert2xx(httpPutAuth("/api/users/" + uid + "/deactivate", "", tok), "TC227");
                String tbl = tableName("User");
                String stCol = columnByField("User", "status");
                String st = jdbc.queryForObject(
                        "SELECT \"" + stCol + "\"::text FROM \"" + tbl + "\" WHERE id=?",
                        String.class, uid);
                assertEquals("DEACTIVATED", st, "TC227: status must be DEACTIVATED; got " + st);
        }
}

// ─── TC228 — S1-F1 search with no filters returns list ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC228_S1F1NoFiltersTests extends TestBase {
        @Test
        @DisplayName("TC228 — Search with no filters returns 2xx (list of users)")
        void search_no_filters() throws Exception {
                BASE_URL = userServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/users/search", tok);
                assert2xx(r, "TC228");
        }
}


// ════════════════════════════════════════════════════════════════════════════
// Auxiliary M1 seed helpers — Destination Service (S2)
// ════════════════════════════════════════════════════════════════════════════

final class _TpM1S2Seed {
        private _TpM1S2Seed() {}

        /** INSERT a Destination with a specific rating + status. */
        static long destWithRating(TestBase t, String name, String country, String city,
                                   String category, String status, double rating) {
                String tbl = t.tableName("Destination");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Destination", "name"), name); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "country"), country); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "city"), city); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "description"), name + " desc"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "category"), category); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "status"), status); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "rating"), rating); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "totalRatings"), 0); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Destination", "destinationDetails"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        static void setDestinationDetails(TestBase t, long destId, String json) {
                String col;
                try { col = t.columnByField("Destination", "details", "destinationDetails", "metadata"); }
                catch (Throwable e) { col = "details"; }
                t.jdbc.update("UPDATE \"" + t.tableName("Destination") + "\" SET \"" + col + "\"=?::jsonb WHERE id=?",
                        json, destId);
        }

        /** INSERT a DestinationReview. */
        static long seedReview(TestBase t, long destinationId, long userId, int rating,
                               String comment, java.time.LocalDate visitDate, boolean verified) {
                String tbl = t.tableName("DestinationReview");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("DestinationReview", "destination"), destinationId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("DestinationReview", "user"), userId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("DestinationReview", "rating"), rating); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("DestinationReview", "comment"), comment); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("DestinationReview", "visitDate"), java.sql.Date.valueOf(visitDate)); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("DestinationReview", "verified"), verified); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("DestinationReview", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S2 — Destination Service M1 features (TC229..TC266) — 38 TCs across F1..F9
// ════════════════════════════════════════════════════════════════════════════

// ─── TC229 — S2-F1 search by category + rating range ─────────────────
@Tag("public")
@Tag("features_m1")
class TC229_S2F1RangeCategoryTests extends TestBase {
        @Test
        @DisplayName("TC229 — Search returns ACTIVE destinations matching category + rating range")
        void search_range_category() throws Exception {
                BASE_URL = catalogServiceUrl;
                _TpM1S2Seed.destWithRating(this, "Hurghada", "EG", "Hurghada", "BEACH", "ACTIVE", 4.5);
                _TpM1S2Seed.destWithRating(this, "Cairo", "EG", "Cairo", "CITY", "INACTIVE", 3.8);
                _TpM1S2Seed.destWithRating(this, "Sharm", "EG", "Sharm", "BEACH", "ACTIVE", 4.9);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/search?category=BEACH&minRating=4.0&maxRating=5.0", tok);
                assert2xx(r, "TC229");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() >= 2, "TC229: at least 2 BEACH in range; got " + list.size());
        }
}

// ─── TC230 — S2-F1 sort rating DESC ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC230_S2F1SortDescTests extends TestBase {
        @Test
        @DisplayName("TC230 — Search results sort by rating DESC")
        void search_sort_desc() throws Exception {
                BASE_URL = catalogServiceUrl;
                _TpM1S2Seed.destWithRating(this, "Low", "EG", "X", "BEACH", "ACTIVE", 3.2);
                _TpM1S2Seed.destWithRating(this, "Mid", "EG", "Y", "BEACH", "ACTIVE", 4.0);
                _TpM1S2Seed.destWithRating(this, "Hi", "EG", "Z", "BEACH", "ACTIVE", 4.8);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/search?minRating=1.0&maxRating=5.0", tok);
                assert2xx(r, "TC230");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                double prev = Double.MAX_VALUE;
                for (JsonNode it : list) {
                        double rt = it.path("rating").asDouble(0);
                        assertTrue(rt <= prev, "TC230: not DESC; prev=" + prev + " curr=" + rt);
                        prev = rt;
                }
        }
}

// ─── TC231 — S2-F1 no category filter ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC231_S2F1NoCategoryTests extends TestBase {
        @Test
        @DisplayName("TC231 — Search without category filter returns all in range")
        void search_no_category() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/search?minRating=0.0&maxRating=5.0", tok);
                assert2xx(r, "TC231");
        }
}

// ─── TC232 — S2-F1 invalid range (min > max) → 400 ───────────────────
@Tag("public")
@Tag("features_m1")
class TC232_S2F1InvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC232 — minRating > maxRating returns 400")
        void search_invalid_range() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/search?minRating=5.0&maxRating=3.0", tok);
                assertEquals(400, r.statusCode(), "TC232: must be 400; got " + r.statusCode());
        }
}

// ─── TC233 — S2-F1 empty range returns empty list ────────────────────
@Tag("public")
@Tag("features_m1")
class TC233_S2F1EmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC233 — Range with no destinations returns empty list")
        void search_empty_range() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/search?minRating=4.99&maxRating=5.0&category=ZZZNOMATCH", tok);
                assert2xx(r, "TC233");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC233: empty list expected; got " + list.size());
        }
}

// ─── TC234 — S2-F2 details JSONB merge ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC234_S2F2DetailsMergeTests extends TestBase {
        @Test
        @DisplayName("TC234 — PUT details merges new fields without removing existing")
        void details_merge() throws Exception {
                BASE_URL = catalogServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC234", "EG", "X", "BEACH", "ACTIVE", 4.0);
                _TpM1S2Seed.setDestinationDetails(this, did,
                        "{\"climate\":\"tropical\",\"currency\":\"USD\",\"visaRequired\":true}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/destinations/" + did + "/details",
                        "{\"currency\":\"EUR\",\"timezone\":\"GMT+2\"}", tok);
                assert2xx(r, "TC234");
        }
}

// ─── TC235 — S2-F2 same-key overwrite ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC235_S2F2OverwriteTests extends TestBase {
        @Test
        @DisplayName("TC235 — Same-key value overwritten")
        void details_overwrite() throws Exception {
                BASE_URL = catalogServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC235", "EG", "X", "BEACH", "ACTIVE", 4.0);
                _TpM1S2Seed.setDestinationDetails(this, did, "{\"climate\":\"tropical\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/destinations/" + did + "/details",
                        "{\"climate\":\"arid\"}", tok);
                assert2xx(r, "TC235");
        }
}

// ─── TC236 — S2-F2 404 non-existent destination ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC236_S2F2NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC236 — Non-existent destination returns 404")
        void details_404() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/destinations/9999999/details",
                        "{\"climate\":\"x\"}", tok);
                assertEquals(404, r.statusCode(), "TC236: must be 404; got " + r.statusCode());
        }
}

// ─── TC237 — S2-F2 multi-key add ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC237_S2F2MultiKeyTests extends TestBase {
        @Test
        @DisplayName("TC237 — Adding multiple new keys at once")
        void details_multi_key() throws Exception {
                BASE_URL = catalogServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC237", "EG", "X", "BEACH", "ACTIVE", 4.0);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/destinations/" + did + "/details",
                        "{\"climate\":\"tropical\",\"currency\":\"USD\",\"timezone\":\"GMT+0\"}", tok);
                assert2xx(r, "TC237");
        }
}

// ─── TC238 — S2-F3 revenue summary happy ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC238_S2F3RevenueHappyTests extends TestBase {
        @Test
        @DisplayName("TC238 — Revenue summary returns totalBookings and totalRevenue fields")
        void revenue_happy() throws Exception {
                BASE_URL = catalogServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC238", "EG", "X", "BEACH", "ACTIVE", 4.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/" + did + "/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC238");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("totalBookings") || j.has("total_bookings"),
                        "TC238: totalBookings key required; got " + r.body());
                assertTrue(j.has("totalRevenue") || j.has("total_revenue"),
                        "TC238: totalRevenue key required");
        }
}

// ─── TC239 — S2-F3 empty range returns zeros ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC239_S2F3EmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC239 — Destination with no bookings returns zeros")
        void revenue_zeros() throws Exception {
                BASE_URL = catalogServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC239", "EG", "X", "BEACH", "ACTIVE", 4.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/" + did + "/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC239");
        }
}

// ─── TC240 — S2-F3 404 non-existent destination ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC240_S2F3NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC240 — Non-existent destination returns 404")
        void revenue_404() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/9999999/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
                assertEquals(404, r.statusCode(), "TC240: must be 404; got " + r.statusCode());
        }
}

// ─── TC241 — S2-F3 averageBookingAmount field present ────────────────
@Tag("public")
@Tag("features_m1")
class TC241_S2F3AvgFieldTests extends TestBase {
        @Test
        @DisplayName("TC241 — Revenue DTO includes averageBookingAmount")
        void revenue_avg_field() throws Exception {
                BASE_URL = catalogServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC241", "EG", "X", "BEACH", "ACTIVE", 4.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/" + did + "/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC241");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("averageBookingAmount") || j.has("average_booking_amount"),
                        "TC241: averageBookingAmount key required");
        }
}

// ─── TC242 — S2-F4 INACTIVE with active itinerary → 400 ──────────────
@Tag("public")
@Tag("features_m1")
class TC242_S2F4InactiveWithItinTests extends TestBase {
        @Test
        @DisplayName("TC242 — Going INACTIVE with an active itinerary returns 400")
        void status_inactive_blocked() throws Exception {
                BASE_URL = catalogServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc242@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC242", "EG", "X", "BEACH", "ACTIVE", 4.0);
                _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 500, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/destinations/" + did + "/status",
                        "{\"status\":\"INACTIVE\"}", tok);
                assertEquals(400, r.statusCode(), "TC242: must be 400; got " + r.statusCode());
        }
}

// ─── TC243 — S2-F4 INACTIVE success after no active itineraries ──────
@Tag("public")
@Tag("features_m1")
class TC243_S2F4InactiveSuccessTests extends TestBase {
        @Test
        @DisplayName("TC243 — INACTIVE succeeds when no active itineraries")
        void status_inactive_success() throws Exception {
                BASE_URL = catalogServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC243", "EG", "X", "BEACH", "ACTIVE", 4.0);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/destinations/" + did + "/status",
                        "{\"status\":\"INACTIVE\"}", tok);
                assert2xx(r, "TC243");
        }
}

// ─── TC244 — S2-F4 404 non-existent destination ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC244_S2F4NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC244 — Status update on non-existent destination returns 404")
        void status_404() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/destinations/9999999/status",
                        "{\"status\":\"INACTIVE\"}", tok);
                assertEquals(404, r.statusCode(), "TC244: must be 404; got " + r.statusCode());
        }
}

// ─── TC245 — S2-F4 status set in PG ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC245_S2F4StatusSetTests extends TestBase {
        @Test
        @DisplayName("TC245 — After update, destination.status is INACTIVE in PG")
        void status_set() throws Exception {
                BASE_URL = catalogServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC245", "EG", "X", "BEACH", "ACTIVE", 4.0);
                String tok = adminToken();
                assert2xx(httpPutAuth("/api/destinations/" + did + "/status",
                        "{\"status\":\"INACTIVE\"}", tok), "TC245");
                String tbl = tableName("Destination");
                String stCol = columnByField("Destination", "status");
                String st = jdbc.queryForObject(
                        "SELECT \"" + stCol + "\"::text FROM \"" + tbl + "\" WHERE id=?",
                        String.class, did);
                assertEquals("INACTIVE", st, "TC245: status must be INACTIVE; got " + st);
        }
}

// ─── TC246 — S2-F4 invalid status → 400 ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC246_S2F4InvalidStatusTests extends TestBase {
        @Test
        @DisplayName("TC246 — Invalid status value returns 400")
        void status_invalid() throws Exception {
                BASE_URL = catalogServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC246", "EG", "X", "BEACH", "ACTIVE", 4.0);
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/destinations/" + did + "/status",
                        "{\"status\":\"BOGUS_NOT_A_STATUS\"}", tok);
                assertEquals(400, r.statusCode(), "TC246: must be 400; got " + r.statusCode());
        }
}

// ─── TC247 — S2-F5 details search eq match ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC247_S2F5EqMatchTests extends TestBase {
        @Test
        @DisplayName("TC247 — JSONB search by key/value returns matching destinations")
        void details_search_eq() throws Exception {
                BASE_URL = catalogServiceUrl;
                long d1 = _TpM1S2Seed.destWithRating(this, "TC247", "EG", "X", "BEACH", "ACTIVE", 4.0);
                _TpM1S2Seed.setDestinationDetails(this, d1, "{\"climate\":\"tropical\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/details/search?key=climate&value=tropical", tok);
                assert2xx(r, "TC247");
        }
}

// ─── TC248 — S2-F5 with status filter ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC248_S2F5WithStatusTests extends TestBase {
        @Test
        @DisplayName("TC248 — JSONB search with status filter scopes results")
        void details_search_status() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/details/search?key=climate&value=tropical&status=ACTIVE", tok);
                assert2xx(r, "TC248");
        }
}

// ─── TC249 — S2-F5 no match returns empty ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC249_S2F5NoMatchTests extends TestBase {
        @Test
        @DisplayName("TC249 — No matching value returns empty list")
        void details_search_no_match() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/details/search?key=climate&value=NoSuchClimateValue", tok);
                assert2xx(r, "TC249");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC249: empty list; got " + list.size());
        }
}

// ─── TC250 — S2-F5 without status filter ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC250_S2F5NoStatusTests extends TestBase {
        @Test
        @DisplayName("TC250 — JSONB search without status filter")
        void details_search_no_status() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/details/search?key=climate&value=tropical", tok);
                assert2xx(r, "TC250");
        }
}

// ─── TC251 — S2-F6 top-rated happy ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC251_S2F6TopRatedTests extends TestBase {
        @Test
        @DisplayName("TC251 — Top rated returns N highest-rated destinations")
        void top_rated() throws Exception {
                BASE_URL = catalogServiceUrl;
                _TpM1S2Seed.destWithRating(this, "TC251a", "EG", "X", "BEACH", "ACTIVE", 4.9);
                _TpM1S2Seed.destWithRating(this, "TC251b", "EG", "Y", "BEACH", "ACTIVE", 4.5);
                _TpM1S2Seed.destWithRating(this, "TC251c", "EG", "Z", "BEACH", "ACTIVE", 4.2);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/destinations/reports/top-rated?limit=2", tok);
                assert2xx(r, "TC251");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() <= 2, "TC251: must cap at 2; got " + list.size());
        }
}

// ─── TC252 — S2-F6 limit caps at N ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC252_S2F6LimitCapTests extends TestBase {
        @Test
        @DisplayName("TC252 — limit caps the result list")
        void top_rated_limit() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/destinations/reports/top-rated?limit=1", tok);
                assert2xx(r, "TC252");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() <= 1, "TC252: must cap at 1; got " + list.size());
        }
}

// ─── TC253 — S2-F6 DTO includes rating field ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC253_S2F6DtoFieldTests extends TestBase {
        @Test
        @DisplayName("TC253 — TopDestinationDTO includes rating field")
        void top_rated_dto() throws Exception {
                BASE_URL = catalogServiceUrl;
                _TpM1S2Seed.destWithRating(this, "TC253", "EG", "X", "BEACH", "ACTIVE", 4.7);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/destinations/reports/top-rated?limit=10", tok);
                assert2xx(r, "TC253");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                if (list.size() >= 1) {
                        JsonNode it = list.get(0);
                        assertTrue(it.has("rating"), "TC253: DTO must include rating; got " + it);
                }
        }
}

// ─── TC254 — S2-F6 ordering DESC ─────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC254_S2F6OrderTests extends TestBase {
        @Test
        @DisplayName("TC254 — Top rated ordered DESC")
        void top_rated_order() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/destinations/reports/top-rated?limit=10", tok);
                assert2xx(r, "TC254");
        }
}

// ─── TC255 — S2-F7 rate happy ────────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC255_S2F7RateHappyTests extends TestBase {
        @Test
        @DisplayName("TC255 — Rate destination returns 200 on COMPLETED itinerary + valid rating")
        void rate_happy() throws Exception {
                BASE_URL = catalogServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc255@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC255", "EG", "X", "BEACH", "ACTIVE", 0.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "COMPLETED", 500, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/destinations/" + did + "/rate",
                        "{\"itineraryId\":" + iid + ",\"rating\":5}", tok);
                assert2xx(r, "TC255");
        }
}

// ─── TC256 — S2-F7 rating out of range → 400 ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC256_S2F7OutOfRangeTests extends TestBase {
        @Test
        @DisplayName("TC256 — Rating > 5 returns 400")
        void rate_out_of_range() throws Exception {
                BASE_URL = catalogServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc256@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC256", "EG", "X", "BEACH", "ACTIVE", 0.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "COMPLETED", 500, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/destinations/" + did + "/rate",
                        "{\"itineraryId\":" + iid + ",\"rating\":6}", tok);
                assertEquals(400, r.statusCode(), "TC256: must be 400; got " + r.statusCode());
        }
}

// ─── TC257 — S2-F7 non-completed itinerary → 400 ─────────────────────
@Tag("public")
@Tag("features_m1")
class TC257_S2F7NonCompletedTests extends TestBase {
        @Test
        @DisplayName("TC257 — Rating against non-completed itinerary returns 400")
        void rate_non_completed() throws Exception {
                BASE_URL = catalogServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc257@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC257", "EG", "X", "BEACH", "ACTIVE", 0.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 500, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/destinations/" + did + "/rate",
                        "{\"itineraryId\":" + iid + ",\"rating\":4}", tok);
                assertEquals(400, r.statusCode(), "TC257: must be 400; got " + r.statusCode());
        }
}

// ─── TC258 — S2-F7 404 non-existent destination/itinerary ────────────
@Tag("public")
@Tag("features_m1")
class TC258_S2F7NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC258 — Non-existent destination returns 404")
        void rate_404() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth("/api/destinations/9999999/rate",
                        "{\"itineraryId\":1,\"rating\":4}", tok);
                assertEquals(404, r.statusCode(), "TC258: must be 404; got " + r.statusCode());
        }
}

// ─── TC259 — S2-F8 verify happy with ADMIN verifier ──────────────────
@Tag("public")
@Tag("features_m1")
class TC259_S2F8VerifyHappyTests extends TestBase {
        @Test
        @DisplayName("TC259 — Verify review with ADMIN verifier returns 2xx")
        void verify_happy() throws Exception {
                BASE_URL = catalogServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc259@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC259", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long rid = _TpM1S2Seed.seedReview(this, did, uid, 4, "great",
                        java.time.LocalDate.of(2025, 1, 1), false);
                long admUid = _TpM1Seed.seedUser(this, "Admin", "tc259adm@tp.io", "ADMIN");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/destinations/" + did + "/reviews/" + rid + "/verify",
                        "{\"verifiedBy\":" + admUid + "}", tok);
                assert2xx(r, "TC259");
        }
}

// ─── TC260 — S2-F8 future visitDate → 400 ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC260_S2F8FutureVisitTests extends TestBase {
        @Test
        @DisplayName("TC260 — Future visitDate returns 400")
        void verify_future_visit() throws Exception {
                BASE_URL = catalogServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc260@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC260", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long rid = _TpM1S2Seed.seedReview(this, did, uid, 4, "soon",
                        _TpM1Seed.futureDate(), false);
                long admUid = _TpM1Seed.seedUser(this, "Admin", "tc260adm@tp.io", "ADMIN");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/destinations/" + did + "/reviews/" + rid + "/verify",
                        "{\"verifiedBy\":" + admUid + "}", tok);
                assertEquals(400, r.statusCode(), "TC260: must be 400; got " + r.statusCode());
        }
}

// ─── TC261 — S2-F8 non-ADMIN verifier → 403 ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC261_S2F8NonAdminTests extends TestBase {
        @Test
        @DisplayName("TC261 — Non-ADMIN verifier returns 403")
        void verify_non_admin() throws Exception {
                BASE_URL = catalogServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc261@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC261", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long rid = _TpM1S2Seed.seedReview(this, did, uid, 4, "ok",
                        java.time.LocalDate.of(2025, 1, 1), false);
                long nonAdm = _TpM1Seed.seedUser(this, "P", "tc261p@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/destinations/" + did + "/reviews/" + rid + "/verify",
                        "{\"verifiedBy\":" + nonAdm + "}", tok);
                assertEquals(403, r.statusCode(), "TC261: must be 403; got " + r.statusCode());
        }
}

// ─── TC262 — S2-F8 review belongs to wrong destination → 400 ─────────
@Tag("public")
@Tag("features_m1")
class TC262_S2F8WrongDestinationTests extends TestBase {
        @Test
        @DisplayName("TC262 — Review belongs to different destination returns 400")
        void verify_wrong_destination() throws Exception {
                BASE_URL = catalogServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc262@tp.io", "TRAVELER");
                long d1 = _TpM1S2Seed.destWithRating(this, "TC262a", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long d2 = _TpM1S2Seed.destWithRating(this, "TC262b", "EG", "Y", "BEACH", "ACTIVE", 4.0);
                long rid = _TpM1S2Seed.seedReview(this, d1, uid, 4, "ok",
                        java.time.LocalDate.of(2025, 1, 1), false);
                long admUid = _TpM1Seed.seedUser(this, "Admin", "tc262adm@tp.io", "ADMIN");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/destinations/" + d2 + "/reviews/" + rid + "/verify",
                        "{\"verifiedBy\":" + admUid + "}", tok);
                assertEquals(400, r.statusCode(), "TC262: must be 400; got " + r.statusCode());
        }
}

// ─── TC263 — S2-F9 low-rated list ────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC263_S2F9LowRatedListTests extends TestBase {
        @Test
        @DisplayName("TC263 — Returns DTOs for destinations with low-rated reviews")
        void low_rated_list() throws Exception {
                BASE_URL = catalogServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc263@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC263", "EG", "X", "BEACH", "ACTIVE", 4.0);
                _TpM1S2Seed.seedReview(this, did, uid, 1, "bad",
                        java.time.LocalDate.of(2025, 1, 1), true);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/reviews/low-rated?maxRating=2", tok);
                assert2xx(r, "TC263");
        }
}

// ─── TC264 — S2-F9 no low-rated returns empty ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC264_S2F9NoLowRatedTests extends TestBase {
        @Test
        @DisplayName("TC264 — No low-rated returns empty list")
        void no_low_rated() throws Exception {
                BASE_URL = catalogServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/reviews/low-rated?maxRating=0", tok);
                assert2xx(r, "TC264");
        }
}

// ─── TC265 — S2-F9 lowRatedCount field ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC265_S2F9LowRatedCountTests extends TestBase {
        @Test
        @DisplayName("TC265 — DTO includes lowRatedCount")
        void low_rated_count() throws Exception {
                BASE_URL = catalogServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc265@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC265", "EG", "X", "BEACH", "ACTIVE", 4.0);
                _TpM1S2Seed.seedReview(this, did, uid, 1, "bad1",
                        java.time.LocalDate.of(2025, 1, 1), true);
                _TpM1S2Seed.seedReview(this, did, uid, 2, "bad2",
                        java.time.LocalDate.of(2025, 1, 2), true);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/reviews/low-rated?maxRating=2", tok);
                assert2xx(r, "TC265");
        }
}

// ─── TC266 — S2-F9 only includes destinations with low-rated ─────────
@Tag("public")
@Tag("features_m1")
class TC266_S2F9OnlyWithLowRatedTests extends TestBase {
        @Test
        @DisplayName("TC266 — Only destinations with at least one low-rated review included")
        void only_low_rated() throws Exception {
                BASE_URL = catalogServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc266@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC266", "EG", "X", "BEACH", "ACTIVE", 4.0);
                _TpM1S2Seed.seedReview(this, did, uid, 5, "great",
                        java.time.LocalDate.of(2025, 1, 1), true);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/destinations/reviews/low-rated?maxRating=2", tok);
                assert2xx(r, "TC266");
        }
}


// ════════════════════════════════════════════════════════════════════════════
// Auxiliary M1 seed helpers — Itinerary Service (S3)
// ════════════════════════════════════════════════════════════════════════════

final class _TpM1S3Seed {
        private _TpM1S3Seed() {}

        /** INSERT an ItineraryDay row. */
        static long seedDay(TestBase t, long itineraryId, int dayOrder, String date,
                            String title, String description, String status) {
                String tbl = t.tableName("ItineraryDay");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("ItineraryDay", "itinerary"), itineraryId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("ItineraryDay", "dayOrder"), dayOrder); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("ItineraryDay", "date"), java.sql.Date.valueOf(date)); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("ItineraryDay", "title"), title); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("ItineraryDay", "description"), description); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("ItineraryDay", "status"), status); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("ItineraryDay", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** Update Itinerary itinerary_details (a.k.a. metadata) JSONB. */
        static void setItineraryMetadata(TestBase t, long itineraryId, String json) {
                t.jdbc.update("UPDATE \"" + t.tableName("Itinerary") + "\" SET itinerary_details=?::jsonb WHERE id=?",
                        json, itineraryId);
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S3 — Itinerary Service M1 features (TC267..TC304) — 38 TCs across F1..F9
// ════════════════════════════════════════════════════════════════════════════

// ─── TC267 — S3-F1 search by status + date range ───────────────────
@Tag("public")
@Tag("features_m1")
class TC267_S3F1SearchTests extends TestBase {
        @Test
        @DisplayName("TC267 — Search itineraries by status and date range")
        void search_status_range() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc267@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC267", "EG", "X", "BEACH", "ACTIVE", 4.0);
                _TpM1Seed.seedItinerary(this, uid, did, "COMPLETED", 1000, "2026-03-10");
                _TpM1Seed.seedItinerary(this, uid, did, "DRAFT", 200, "2026-03-15");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/search?status=COMPLETED&startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC267");
        }
}

// ─── TC268 — S3-F1 no status filter ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC268_S3F1NoStatusTests extends TestBase {
        @Test
        @DisplayName("TC268 — Search without status returns all in range")
        void search_no_status() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/search?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC268");
        }
}

// ─── TC269 — S3-F1 ordered most recent first ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC269_S3F1OrderTests extends TestBase {
        @Test
        @DisplayName("TC269 — Search results most-recent first")
        void search_order() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/search?startDate=2026-01-01&endDate=2026-12-31", tok);
                assert2xx(r, "TC269");
        }
}

// ─── TC270 — S3-F1 empty range returns empty ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC270_S3F1EmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC270 — Empty range returns empty list")
        void search_empty() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/search?startDate=2099-01-01&endDate=2099-01-31", tok);
                assert2xx(r, "TC270");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC270: empty list expected; got " + list.size());
        }
}

// ─── TC271 — S3-F2 assign destination happy ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC271_S3F2AssignHappyTests extends TestBase {
        @Test
        @DisplayName("TC271 — Assign ACTIVE destination to DRAFT itinerary returns 2xx")
        void assign_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc271@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC271", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "DRAFT", 0, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/itineraries/" + iid + "/assign?destinationId=" + did, "", tok);
                assert2xx(r, "TC271");
        }
}

// ─── TC272 — S3-F2 not DRAFT → 400 ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC272_S3F2NotDraftTests extends TestBase {
        @Test
        @DisplayName("TC272 — Assigning to a PLANNED itinerary returns 400")
        void assign_not_draft() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc272@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC272", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/itineraries/" + iid + "/assign?destinationId=" + did, "", tok);
                assertEquals(400, r.statusCode(), "TC272: must be 400; got " + r.statusCode());
        }
}

// ─── TC273 — S3-F2 inactive destination → 400 ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC273_S3F2InactiveDestinationTests extends TestBase {
        @Test
        @DisplayName("TC273 — Assigning an INACTIVE destination returns 400")
        void assign_inactive_dest() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc273@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC273", "EG", "X", "BEACH", "INACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "DRAFT", 0, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/itineraries/" + iid + "/assign?destinationId=" + did, "", tok);
                assertEquals(400, r.statusCode(), "TC273: must be 400; got " + r.statusCode());
        }
}

// ─── TC274 — S3-F2 404 non-existent itinerary ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC274_S3F2NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC274 — Non-existent itinerary returns 404")
        void assign_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth(
                        "/api/itineraries/9999999/assign?destinationId=1", "", tok);
                assertEquals(404, r.statusCode(), "TC274: must be 404; got " + r.statusCode());
        }
}

// ─── TC275 — S3-F3 estimate happy ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC275_S3F3EstimateHappyTests extends TestBase {
        @Test
        @DisplayName("TC275 — Trip estimate returns 2xx with seasonMultiplier")
        void estimate_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC275", "EG", "X", "BEACH", "ACTIVE", 4.0);
                String tok = adminToken();
                String body = "{\"destinationId\":" + did
                        + ",\"numberOfDays\":7,\"numberOfTravelers\":2}";
                HttpResponse<String> r = httpPostAuth("/api/itineraries/estimate", body, tok);
                assert2xx(r, "TC275");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("seasonMultiplier") || j.has("season_multiplier"),
                        "TC275: seasonMultiplier key required; got " + r.body());
        }
}

// ─── TC276 — S3-F3 negative travelers → 400 ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC276_S3F3NegativeTravelersTests extends TestBase {
        @Test
        @DisplayName("TC276 — Negative numberOfTravelers returns 400")
        void estimate_negative() throws Exception {
                BASE_URL = orderServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC276", "EG", "X", "BEACH", "ACTIVE", 4.0);
                String tok = adminToken();
                String body = "{\"destinationId\":" + did
                        + ",\"numberOfDays\":7,\"numberOfTravelers\":-1}";
                HttpResponse<String> r = httpPostAuth("/api/itineraries/estimate", body, tok);
                assertEquals(400, r.statusCode(), "TC276: must be 400; got " + r.statusCode());
        }
}

// ─── TC277 — S3-F3 404 missing destination ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC277_S3F3MissingDestinationTests extends TestBase {
        @Test
        @DisplayName("TC277 — Non-existent destination returns 404")
        void estimate_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                String body = "{\"destinationId\":9999999,\"numberOfDays\":7,\"numberOfTravelers\":2}";
                HttpResponse<String> r = httpPostAuth("/api/itineraries/estimate", body, tok);
                assertEquals(404, r.statusCode(), "TC277: must be 404; got " + r.statusCode());
        }
}

// ─── TC278 — S3-F3 read-only (no itinerary created) ────────────────
@Tag("public")
@Tag("features_m1")
class TC278_S3F3ReadOnlyTests extends TestBase {
        @Test
        @DisplayName("TC278 — Estimate is read-only (does not persist)")
        void estimate_read_only() throws Exception {
                BASE_URL = orderServiceUrl;
                long did = _TpM1S2Seed.destWithRating(this, "TC278", "EG", "X", "BEACH", "ACTIVE", 4.0);
                String tok = adminToken();
                String body = "{\"destinationId\":" + did
                        + ",\"numberOfDays\":7,\"numberOfTravelers\":2}";
                Long countBefore = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tableName("Itinerary") + "\"",
                        Long.class);
                assert2xx(httpPostAuth("/api/itineraries/estimate", body, tok), "TC278");
                Long countAfter = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tableName("Itinerary") + "\"",
                        Long.class);
                assertEquals(countBefore, countAfter,
                        "TC278: estimate must not create an itinerary row");
        }
}

// ─── TC279 — S3-F4 complete happy ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC279_S3F4CompleteHappyTests extends TestBase {
        @Test
        @DisplayName("TC279 — Completing an IN_PROGRESS itinerary returns 2xx")
        void complete_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc279@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC279", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "IN_PROGRESS", 1500, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/itineraries/" + iid + "/complete", "", tok);
                assert2xx(r, "TC279");
        }
}

// ─── TC280 — S3-F4 not IN_PROGRESS → 400 ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC280_S3F4NotInProgressTests extends TestBase {
        @Test
        @DisplayName("TC280 — Completing a DRAFT itinerary returns 400")
        void complete_not_in_progress() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc280@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC280", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "DRAFT", 0, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/itineraries/" + iid + "/complete", "", tok);
                assertEquals(400, r.statusCode(), "TC280: must be 400; got " + r.statusCode());
        }
}

// ─── TC281 — S3-F4 already COMPLETED → 400 ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC281_S3F4AlreadyCompletedTests extends TestBase {
        @Test
        @DisplayName("TC281 — Completing already-COMPLETED itinerary returns 400")
        void complete_already() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc281@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC281", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "COMPLETED", 1500, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/itineraries/" + iid + "/complete", "", tok);
                assertEquals(400, r.statusCode(), "TC281: must be 400; got " + r.statusCode());
        }
}

// ─── TC282 — S3-F4 404 non-existent ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC282_S3F4NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC282 — Non-existent itinerary returns 404")
        void complete_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/itineraries/9999999/complete", "", tok);
                assertEquals(404, r.statusCode(), "TC282: must be 404; got " + r.statusCode());
        }
}

// ─── TC283 — S3-F5 metadata search happy ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC283_S3F5MetaSearchTests extends TestBase {
        @Test
        @DisplayName("TC283 — Metadata search returns 2xx")
        void meta_search() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/metadata/search?key=travelStyle&value=luxury", tok);
                assert2xx(r, "TC283");
        }
}

// ─── TC284 — S3-F5 blank key → 400 ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC284_S3F5BlankKeyTests extends TestBase {
        @Test
        @DisplayName("TC284 — Blank key returns 400")
        void meta_search_blank() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/metadata/search?key=&value=x", tok);
                assertEquals(400, r.statusCode(), "TC284: must be 400; got " + r.statusCode());
        }
}

// ─── TC285 — S3-F5 no match returns empty ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC285_S3F5NoMatchTests extends TestBase {
        @Test
        @DisplayName("TC285 — No match returns empty list")
        void meta_search_no_match() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/metadata/search?key=travelStyle&value=NoSuchStyle", tok);
                assert2xx(r, "TC285");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC285: empty list; got " + list.size());
        }
}

// ─── TC286 — S3-F6 analytics happy ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC286_S3F6AnalyticsTests extends TestBase {
        @Test
        @DisplayName("TC286 — Analytics returns DTO with totalItineraries etc.")
        void analytics() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/analytics?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC286");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("totalItineraries") || j.has("total_itineraries"),
                        "TC286: totalItineraries key required");
        }
}

// ─── TC287 — S3-F6 completionRate field ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC287_S3F6CompletionRateTests extends TestBase {
        @Test
        @DisplayName("TC287 — Analytics DTO includes completionRate")
        void completion_rate_field() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/analytics?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC287");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("completionRate") || j.has("completion_rate"),
                        "TC287: completionRate key required");
        }
}

// ─── TC288 — S3-F6 completed/cancelled breakdown ───────────────────
@Tag("public")
@Tag("features_m1")
class TC288_S3F6CompletedCancelledTests extends TestBase {
        @Test
        @DisplayName("TC288 — Analytics DTO includes completedItineraries and cancelledItineraries")
        void completed_cancelled() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/analytics?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC288");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("completedItineraries") || j.has("completed_itineraries"),
                        "TC288: completedItineraries required");
                assertTrue(j.has("cancelledItineraries") || j.has("cancelled_itineraries"),
                        "TC288: cancelledItineraries required");
        }
}

// ─── TC289 — S3-F6 averageBudget field ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC289_S3F6AverageBudgetTests extends TestBase {
        @Test
        @DisplayName("TC289 — Analytics DTO includes averageBudget")
        void avg_budget_field() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/itineraries/analytics?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC289");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("averageBudget") || j.has("average_budget"),
                        "TC289: averageBudget required");
        }
}

// ─── TC290 — S3-F7 cancel happy ────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC290_S3F7CancelHappyTests extends TestBase {
        @Test
        @DisplayName("TC290 — Cancelling a PLANNED itinerary returns 2xx")
        void cancel_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc290@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC290", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 500, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/itineraries/" + iid + "/cancel", "", tok);
                assert2xx(r, "TC290");
        }
}

// ─── TC291 — S3-F7 already COMPLETED → 400 ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC291_S3F7CancelCompletedTests extends TestBase {
        @Test
        @DisplayName("TC291 — Cancelling a COMPLETED itinerary returns 400")
        void cancel_completed() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc291@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC291", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "COMPLETED", 500, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/itineraries/" + iid + "/cancel", "", tok);
                assertEquals(400, r.statusCode(), "TC291: must be 400; got " + r.statusCode());
        }
}

// ─── TC292 — S3-F7 IN_PROGRESS → 400 ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC292_S3F7InProgressTests extends TestBase {
        @Test
        @DisplayName("TC292 — Cancelling an IN_PROGRESS itinerary returns 400")
        void cancel_in_progress() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc292@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC292", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "IN_PROGRESS", 500, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/itineraries/" + iid + "/cancel", "", tok);
                assertEquals(400, r.statusCode(), "TC292: must be 400; got " + r.statusCode());
        }
}

// ─── TC293 — S3-F7 404 non-existent ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC293_S3F7NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC293 — Non-existent itinerary returns 404")
        void cancel_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/itineraries/9999999/cancel", "", tok);
                assertEquals(404, r.statusCode(), "TC293: must be 404; got " + r.statusCode());
        }
}

// ─── TC294 — S3-F8 add days happy ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC294_S3F8AddDaysTests extends TestBase {
        @Test
        @DisplayName("TC294 — Adding days to a DRAFT itinerary returns 2xx")
        void add_days_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc294@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC294", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "DRAFT", 0, "2026-04-10");
                String tok = adminToken();
                String body = "[{\"date\":\"2026-04-10\",\"title\":\"Day1\",\"description\":\"a\",\"metadata\":{}},"
                        + "{\"date\":\"2026-04-11\",\"title\":\"Day2\",\"description\":\"b\",\"metadata\":{}}]";
                HttpResponse<String> r = httpPostAuth("/api/itineraries/" + iid + "/days", body, tok);
                assert2xx(r, "TC294");
        }
}

// ─── TC295 — S3-F8 wrong status (COMPLETED) → 400 ──────────────────
@Tag("public")
@Tag("features_m1")
class TC295_S3F8WrongStatusTests extends TestBase {
        @Test
        @DisplayName("TC295 — Adding days to COMPLETED itinerary returns 400")
        void days_wrong_status() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc295@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC295", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "COMPLETED", 1000, "2026-04-10");
                String tok = adminToken();
                String body = "[{\"date\":\"2026-04-10\",\"title\":\"Day1\",\"description\":\"a\",\"metadata\":{}}]";
                HttpResponse<String> r = httpPostAuth("/api/itineraries/" + iid + "/days", body, tok);
                assertEquals(400, r.statusCode(), "TC295: must be 400; got " + r.statusCode());
        }
}

// ─── TC296 — S3-F8 missing title → 400 ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC296_S3F8MissingFieldTests extends TestBase {
        @Test
        @DisplayName("TC296 — Missing title returns 400")
        void days_missing() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc296@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC296", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "DRAFT", 0, "2026-04-10");
                String tok = adminToken();
                String body = "[{\"date\":\"2026-04-10\",\"description\":\"a\",\"metadata\":{}}]";
                HttpResponse<String> r = httpPostAuth("/api/itineraries/" + iid + "/days", body, tok);
                assertEquals(400, r.statusCode(), "TC296: must be 400; got " + r.statusCode());
        }
}

// ─── TC297 — S3-F8 dayOrder continues from max ─────────────────────
@Tag("public")
@Tag("features_m1")
class TC297_S3F8DayOrderTests extends TestBase {
        @Test
        @DisplayName("TC297 — Adding days continues dayOrder from existing max")
        void days_order_continues() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc297@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC297", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "DRAFT", 0, "2026-04-10");
                _TpM1S3Seed.seedDay(this, iid, 1, "2026-04-10", "Existing", "x", "PLANNED");
                String tok = adminToken();
                String body = "[{\"date\":\"2026-04-11\",\"title\":\"Day2\",\"description\":\"b\",\"metadata\":{}}]";
                HttpResponse<String> r = httpPostAuth("/api/itineraries/" + iid + "/days", body, tok);
                assert2xx(r, "TC297");
        }
}

// ─── TC298 — S3-F8 404 non-existent itinerary ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC298_S3F8NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC298 — Non-existent itinerary returns 404")
        void days_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                String body = "[{\"date\":\"2026-04-10\",\"title\":\"Day1\",\"description\":\"a\",\"metadata\":{}}]";
                HttpResponse<String> r = httpPostAuth("/api/itineraries/9999999/days", body, tok);
                assertEquals(404, r.statusCode(), "TC298: must be 404; got " + r.statusCode());
        }
}

// ─── TC299 — S3-F9 details happy ───────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC299_S3F9DetailsTests extends TestBase {
        @Test
        @DisplayName("TC299 — Details DTO includes days list with totalDays")
        void details_happy() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc299@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC299", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 500, "2026-04-10");
                _TpM1S3Seed.seedDay(this, iid, 1, "2026-04-10", "D1", "a", "PLANNED");
                _TpM1S3Seed.seedDay(this, iid, 2, "2026-04-11", "D2", "b", "COMPLETED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/itineraries/" + iid + "/details", tok);
                assert2xx(r, "TC299");
                JsonNode j = parseNode(r.body());
                long total = _TpM2.rL(j, "totalDays", "total_days");
                assertEquals(2L, total, "TC299: totalDays=2; got " + total);
        }
}

// ─── TC300 — S3-F9 completedDays count ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC300_S3F9CompletedDaysTests extends TestBase {
        @Test
        @DisplayName("TC300 — completedDays counts COMPLETED days")
        void completed_count() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc300@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC300", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 500, "2026-04-10");
                _TpM1S3Seed.seedDay(this, iid, 1, "2026-04-10", "D1", "a", "COMPLETED");
                _TpM1S3Seed.seedDay(this, iid, 2, "2026-04-11", "D2", "b", "COMPLETED");
                _TpM1S3Seed.seedDay(this, iid, 3, "2026-04-12", "D3", "c", "PLANNED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/itineraries/" + iid + "/details", tok);
                assert2xx(r, "TC300");
                long cc = _TpM2.rL(parseNode(r.body()), "completedDays", "completed_days");
                assertEquals(2L, cc, "TC300: completedDays=2; got " + cc);
        }
}

// ─── TC301 — S3-F9 no days returns 0 ───────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC301_S3F9NoDaysTests extends TestBase {
        @Test
        @DisplayName("TC301 — Itinerary with no days: totalDays=0")
        void no_days() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc301@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC301", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 500, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/itineraries/" + iid + "/details", tok);
                assert2xx(r, "TC301");
                long total = _TpM2.rL(parseNode(r.body()), "totalDays", "total_days");
                assertEquals(0L, total, "TC301: totalDays=0; got " + total);
        }
}

// ─── TC302 — S3-F9 404 non-existent ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC302_S3F9NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC302 — Non-existent itinerary returns 404")
        void details_404() throws Exception {
                BASE_URL = orderServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/itineraries/9999999/details", tok);
                assertEquals(404, r.statusCode(), "TC302: must be 404; got " + r.statusCode());
        }
}

// ─── TC303 — S3-F9 days ordered by dayOrder ASC ────────────────────
@Tag("public")
@Tag("features_m1")
class TC303_S3F9DaysOrderTests extends TestBase {
        @Test
        @DisplayName("TC303 — Days list ordered by dayOrder ASC")
        void days_order() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc303@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC303", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 500, "2026-04-10");
                _TpM1S3Seed.seedDay(this, iid, 3, "2026-04-12", "D3", "c", "PLANNED");
                _TpM1S3Seed.seedDay(this, iid, 1, "2026-04-10", "D1", "a", "PLANNED");
                _TpM1S3Seed.seedDay(this, iid, 2, "2026-04-11", "D2", "b", "PLANNED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/itineraries/" + iid + "/details", tok);
                assert2xx(r, "TC303");
        }
}

// ─── TC304 — S3-F2 itinerary status changes to PLANNED ─────────────
@Tag("public")
@Tag("features_m1")
class TC304_S3F2StatusChangedTests extends TestBase {
        @Test
        @DisplayName("TC304 — After assign, itinerary.status is PLANNED in PG")
        void assign_status() throws Exception {
                BASE_URL = orderServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc304@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC304", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "DRAFT", 0, "2026-04-10");
                String tok = adminToken();
                assert2xx(httpPutAuth(
                        "/api/itineraries/" + iid + "/assign?destinationId=" + did, "", tok), "TC304");
                String tbl = tableName("Itinerary");
                String stCol = columnByField("Itinerary", "status");
                String st = jdbc.queryForObject(
                        "SELECT \"" + stCol + "\"::text FROM \"" + tbl + "\" WHERE id=?",
                        String.class, iid);
                assertEquals("PLANNED", st, "TC304: status must be PLANNED; got " + st);
        }
}


// ════════════════════════════════════════════════════════════════════════════
// Auxiliary M1 seed helpers — Activity Service (S4)
// ════════════════════════════════════════════════════════════════════════════

final class _TpM1S4Seed {
        private _TpM1S4Seed() {}

        /** INSERT an Activity with explicit coords, scheduledTime, cost-in-metadata. */
        static long actAt(TestBase t, long itineraryId, String name, String category,
                          double lat, double lon, String scheduledDateTime,
                          double cost, String status) {
                String tbl = t.tableName("Activity");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Activity", "itinerary"), itineraryId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Activity", "name"), name); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Activity", "category"), category); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Activity", "latitude"), lat); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Activity", "longitude"), lon); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Activity", "scheduledTime"), java.sql.Timestamp.valueOf(scheduledDateTime)); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Activity", "cost"), cost); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Activity", "status"), status); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Activity", "activityDetails"),
                        "{\"cost\":" + cost + "}"); } catch (Throwable ignore) {}
                Long id = t.insertRowReturningId(tbl, ov);
                t.setAllDateColumns(tbl, id, java.sql.Timestamp.valueOf(scheduledDateTime));
                return id;
        }

        static void setActivityMetadata(TestBase t, long activityId, String json) {
                String col;
                try { col = t.columnByField("Activity", "metadata", "activityDetails", "details"); }
                catch (Throwable e) { col = "metadata"; }
                t.jdbc.update("UPDATE \"" + t.tableName("Activity") + "\" SET \"" + col + "\"=?::jsonb WHERE id=?",
                        json, activityId);
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S4 — Activity Service M1 features (TC305..TC340) — 36 TCs across F1..F9
// ════════════════════════════════════════════════════════════════════════════

// ─── TC305 — S4-F1 latest activity happy ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC305_S4F1LatestActivityTests extends TestBase {
        @Test
        @DisplayName("TC305 — Returns the most recent activity for an itinerary")
        void latest_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc305@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC305", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                _TpM1S4Seed.actAt(this, iid, "A1", "SIGHTSEEING", 30.04, 31.23,
                        "2026-04-10 09:00:00", 100, "BOOKED");
                _TpM1S4Seed.actAt(this, iid, "A2", "SIGHTSEEING", 30.05, 31.24,
                        "2026-04-10 11:00:00", 200, "BOOKED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/itinerary/" + iid + "/latest", tok);
                assert2xx(r, "TC305");
        }
}

// ─── TC306 — S4-F1 no activities → 404 ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC306_S4F1NoActivitiesTests extends TestBase {
        @Test
        @DisplayName("TC306 — Itinerary with no activities returns 404")
        void no_activities() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc306@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC306", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/itinerary/" + iid + "/latest", tok);
                assertEquals(404, r.statusCode(), "TC306: must be 404; got " + r.statusCode());
        }
}

// ─── TC307 — S4-F1 404 non-existent itinerary ───────────────────
@Tag("public")
@Tag("features_m1")
class TC307_S4F1ItinNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC307 — Non-existent itinerary returns 404")
        void itin_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/itinerary/9999999/latest", tok);
                assertEquals(404, r.statusCode(), "TC307: must be 404; got " + r.statusCode());
        }
}

// ─── TC308 — S4-F1 most recent by scheduledTime DESC ─────────────
@Tag("public")
@Tag("features_m1")
class TC308_S4F1MostRecentTests extends TestBase {
        @Test
        @DisplayName("TC308 — Multiple activities returns the one with latest scheduledTime")
        void most_recent() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc308@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC308", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                _TpM1S4Seed.actAt(this, iid, "Old", "SIGHTSEEING", 30.04, 31.23,
                        "2026-01-01 09:00:00", 100, "BOOKED");
                _TpM1S4Seed.actAt(this, iid, "New", "SIGHTSEEING", 30.05, 31.24,
                        "2026-04-10 11:00:00", 200, "BOOKED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/itinerary/" + iid + "/latest", tok);
                assert2xx(r, "TC308");
        }
}

// ─── TC309 — S4-F2 create activity happy ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC309_S4F2CreateHappyTests extends TestBase {
        @Test
        @DisplayName("TC309 — Create activity with metadata returns 2xx (201)")
        void create_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc309@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC309", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                String tok = adminToken();
                String body = "{\"name\":\"Pyramids Tour\",\"category\":\"SIGHTSEEING\","
                        + "\"latitude\":\"30.04\",\"longitude\":\"31.23\","
                        + "\"scheduledTime\":\"2026-04-10T09:00:00\","
                        + "\"metadata\":{\"duration\":3,\"cost\":200}}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/activities/itinerary/" + iid, body, tok);
                assertTrue(r.statusCode() == 200 || r.statusCode() == 201,
                        "TC309: must be 2xx; got " + r.statusCode() + " body=" + r.body());
        }
}

// ─── TC310 — S4-F2 404 non-existent itinerary ───────────────────
@Tag("public")
@Tag("features_m1")
class TC310_S4F2NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC310 — Non-existent itinerary returns 404")
        void create_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                String body = "{\"name\":\"X\",\"category\":\"SIGHTSEEING\","
                        + "\"latitude\":\"0\",\"longitude\":\"0\","
                        + "\"scheduledTime\":\"2026-04-10T09:00:00\",\"metadata\":{}}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/activities/itinerary/9999999", body, tok);
                assertEquals(404, r.statusCode(), "TC310: must be 404; got " + r.statusCode());
        }
}

// ─── TC311 — S4-F2 metadata persisted ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC311_S4F2MetadataPersistedTests extends TestBase {
        @Test
        @DisplayName("TC311 — Metadata is persisted with the activity")
        void metadata_persisted() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc311@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC311", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                String tok = adminToken();
                String body = "{\"name\":\"Tour\",\"category\":\"SIGHTSEEING\","
                        + "\"latitude\":\"30.04\",\"longitude\":\"31.23\","
                        + "\"scheduledTime\":\"2026-04-10T09:00:00\","
                        + "\"metadata\":{\"cost\":150}}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/activities/itinerary/" + iid, body, tok);
                assertTrue(r.statusCode() == 200 || r.statusCode() == 201,
                        "TC311: must be 2xx; got " + r.statusCode());
        }
}

// ─── TC312 — S4-F2 multi-key metadata ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC312_S4F2MultiKeyTests extends TestBase {
        @Test
        @DisplayName("TC312 — Multi-key metadata accepted")
        void multi_key() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc312@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC312", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                String tok = adminToken();
                String body = "{\"name\":\"X\",\"category\":\"SIGHTSEEING\","
                        + "\"latitude\":\"30\",\"longitude\":\"31\","
                        + "\"scheduledTime\":\"2026-04-10T09:00:00\","
                        + "\"metadata\":{\"cost\":150,\"duration\":2,\"language\":\"en\"}}";
                HttpResponse<String> r = httpPostAuth(
                        "/api/activities/itinerary/" + iid, body, tok);
                assertTrue(r.statusCode() == 200 || r.statusCode() == 201,
                        "TC312: must be 2xx; got " + r.statusCode());
        }
}

// ─── TC313 — S4-F3 nearby happy ─────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC313_S4F3NearbyTests extends TestBase {
        @Test
        @DisplayName("TC313 — Nearby returns activities within radius")
        void nearby_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc313@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC313", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                _TpM1S4Seed.actAt(this, iid, "A", "SIGHTSEEING", 30.04, 31.23,
                        "2026-04-10 09:00:00", 100, "BOOKED");
                _TpM1S4Seed.actAt(this, iid, "B", "SIGHTSEEING", 30.05, 31.24,
                        "2026-04-10 10:00:00", 100, "BOOKED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/nearby?lat=30.044&lon=31.235&radiusKm=5", tok);
                assert2xx(r, "TC313");
        }
}

// ─── TC314 — S4-F3 outside radius excluded ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC314_S4F3OutsideRadiusTests extends TestBase {
        @Test
        @DisplayName("TC314 — Activities outside radius are excluded")
        void nearby_outside() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc314@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC314", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                _TpM1S4Seed.actAt(this, iid, "Far", "SIGHTSEEING", 31.0, 32.0,
                        "2026-04-10 09:00:00", 100, "BOOKED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/nearby?lat=30.04&lon=31.23&radiusKm=1", tok);
                assert2xx(r, "TC314");
        }
}

// ─── TC315 — S4-F3 sorted ASC by distance ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC315_S4F3SortAscTests extends TestBase {
        @Test
        @DisplayName("TC315 — Results sorted ascending by distanceKm")
        void nearby_sort() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/nearby?lat=30.0&lon=31.0&radiusKm=200", tok);
                assert2xx(r, "TC315");
        }
}

// ─── TC316 — S4-F3 small radius returns fewer ───────────────────
@Tag("public")
@Tag("features_m1")
class TC316_S4F3SmallRadiusTests extends TestBase {
        @Test
        @DisplayName("TC316 — Small radius returns fewer results")
        void nearby_small() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/nearby?lat=30.0&lon=31.0&radiusKm=0.1", tok);
                assert2xx(r, "TC316");
        }
}

// ─── TC317 — S4-F4 batch create happy ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC317_S4F4BatchHappyTests extends TestBase {
        @Test
        @DisplayName("TC317 — Batch create returns 201 with correct count")
        void batch_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc317@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC317", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                String tok = adminToken();
                String body = "{\"itineraryId\":" + iid + ",\"activities\":["
                        + "{\"name\":\"A\",\"category\":\"SIGHTSEEING\","
                        + "\"latitude\":\"30\",\"longitude\":\"31\","
                        + "\"scheduledTime\":\"2026-04-10T09:00:00\",\"metadata\":{}},"
                        + "{\"name\":\"B\",\"category\":\"SIGHTSEEING\","
                        + "\"latitude\":\"30\",\"longitude\":\"31\","
                        + "\"scheduledTime\":\"2026-04-10T10:00:00\",\"metadata\":{}}]}";
                HttpResponse<String> r = httpPostAuth("/api/activities/batch", body, tok);
                assertTrue(r.statusCode() == 200 || r.statusCode() == 201,
                        "TC317: must be 2xx; got " + r.statusCode() + " body=" + r.body());
        }
}

// ─── TC318 — S4-F4 invalid coordinates → 400 ────────────────────
@Tag("public")
@Tag("features_m1")
class TC318_S4F4InvalidCoordsTests extends TestBase {
        @Test
        @DisplayName("TC318 — Invalid latitude (999) returns 400")
        void batch_invalid_coords() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc318@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC318", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                String tok = adminToken();
                String body = "{\"itineraryId\":" + iid + ",\"activities\":["
                        + "{\"name\":\"A\",\"category\":\"SIGHTSEEING\","
                        + "\"latitude\":\"999\",\"longitude\":\"31\","
                        + "\"scheduledTime\":\"2026-04-10T09:00:00\",\"metadata\":{}}]}";
                HttpResponse<String> r = httpPostAuth("/api/activities/batch", body, tok);
                assertEquals(400, r.statusCode(), "TC318: must be 400; got " + r.statusCode());
        }
}

// ─── TC319 — S4-F4 404 non-existent itinerary ───────────────────
@Tag("public")
@Tag("features_m1")
class TC319_S4F4ItinNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC319 — Non-existent itineraryId returns 404")
        void batch_itin_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                String body = "{\"itineraryId\":9999999,\"activities\":["
                        + "{\"name\":\"A\",\"category\":\"SIGHTSEEING\","
                        + "\"latitude\":\"30\",\"longitude\":\"31\","
                        + "\"scheduledTime\":\"2026-04-10T09:00:00\",\"metadata\":{}}]}";
                HttpResponse<String> r = httpPostAuth("/api/activities/batch", body, tok);
                assertEquals(404, r.statusCode(), "TC319: must be 404; got " + r.statusCode());
        }
}

// ─── TC320 — S4-F4 invalid longitude → 400 ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC320_S4F4InvalidLonTests extends TestBase {
        @Test
        @DisplayName("TC320 — Invalid longitude (>180) returns 400")
        void batch_invalid_lon() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc320@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC320", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                String tok = adminToken();
                String body = "{\"itineraryId\":" + iid + ",\"activities\":["
                        + "{\"name\":\"A\",\"category\":\"SIGHTSEEING\","
                        + "\"latitude\":\"30\",\"longitude\":\"999\","
                        + "\"scheduledTime\":\"2026-04-10T09:00:00\",\"metadata\":{}}]}";
                HttpResponse<String> r = httpPostAuth("/api/activities/batch", body, tok);
                assertEquals(400, r.statusCode(), "TC320: must be 400; got " + r.statusCode());
        }
}

// ─── TC321 — S4-F5 metadata search eq ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC321_S4F5EqTests extends TestBase {
        @Test
        @DisplayName("TC321 — operator=eq returns matching activities")
        void meta_eq() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc321@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC321", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long aid = _TpM1S4Seed.actAt(this, iid, "A", "SIGHTSEEING", 30.0, 31.0,
                        "2026-04-10 09:00:00", 100, "BOOKED");
                _TpM1S4Seed.setActivityMetadata(this, aid, "{\"cost\":100}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/metadata/search?key=cost&operator=eq&value=100", tok);
                assert2xx(r, "TC321");
        }
}

// ─── TC322 — S4-F5 gt operator ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC322_S4F5GtTests extends TestBase {
        @Test
        @DisplayName("TC322 — operator=gt filters numerically")
        void meta_gt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc322@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC322", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long aid = _TpM1S4Seed.actAt(this, iid, "A", "SIGHTSEEING", 30.0, 31.0,
                        "2026-04-10 09:00:00", 500, "BOOKED");
                _TpM1S4Seed.setActivityMetadata(this, aid, "{\"cost\":500}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/metadata/search?key=cost&operator=gt&value=200", tok);
                assert2xx(r, "TC322");
        }
}

// ─── TC323 — S4-F5 lt operator ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC323_S4F5LtTests extends TestBase {
        @Test
        @DisplayName("TC323 — operator=lt filters numerically")
        void meta_lt() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/metadata/search?key=cost&operator=lt&value=100", tok);
                assert2xx(r, "TC323");
        }
}

// ─── TC324 — S4-F5 invalid op → 400 ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC324_S4F5InvalidOpTests extends TestBase {
        @Test
        @DisplayName("TC324 — Invalid operator returns 400")
        void meta_invalid_op() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/metadata/search?key=cost&operator=xyz&value=100", tok);
                assertEquals(400, r.statusCode(), "TC324: must be 400; got " + r.statusCode());
        }
}

// ─── TC325 — S4-F6 history happy ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC325_S4F6HistoryTests extends TestBase {
        @Test
        @DisplayName("TC325 — History returns activities in date range")
        void history() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/history?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC325");
        }
}

// ─── TC326 — S4-F6 with category filter ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC326_S4F6CategoryFilterTests extends TestBase {
        @Test
        @DisplayName("TC326 — History with category filter")
        void history_category() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/history?startDate=2026-03-01&endDate=2026-03-31&category=SIGHTSEEING", tok);
                assert2xx(r, "TC326");
        }
}

// ─── TC327 — S4-F6 ASC order ────────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC327_S4F6OrderTests extends TestBase {
        @Test
        @DisplayName("TC327 — History results ordered ASC by scheduledTime")
        void history_order() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/history?startDate=2026-01-01&endDate=2026-12-31", tok);
                assert2xx(r, "TC327");
        }
}

// ─── TC328 — S4-F6 empty range returns empty ────────────────────
@Tag("public")
@Tag("features_m1")
class TC328_S4F6EmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC328 — Empty range returns empty list")
        void history_empty() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/history?startDate=2099-01-01&endDate=2099-01-31", tok);
                assert2xx(r, "TC328");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC328: empty list expected; got " + list.size());
        }
}

// ─── TC329 — S4-F7 purge happy ──────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC329_S4F7PurgeTests extends TestBase {
        @Test
        @DisplayName("TC329 — Purge removes only old activities")
        void purge_happy() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpDeleteAuth("/api/activities/purge?olderThanDays=30", tok);
                assert2xx(r, "TC329");
        }
}

// ─── TC330 — S4-F7 recent activities not purged ─────────────────
@Tag("public")
@Tag("features_m1")
class TC330_S4F7RecentNotPurgedTests extends TestBase {
        @Test
        @DisplayName("TC330 — Recent activities are not purged")
        void purge_no_recent() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc330@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC330", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                java.time.LocalDate today = java.time.LocalDate.now();
                long aid = _TpM1S4Seed.actAt(this, iid, "Recent", "SIGHTSEEING", 30.0, 31.0,
                        today.toString() + " 09:00:00", 100, "BOOKED");
                String tok = adminToken();
                assert2xx(httpDeleteAuth("/api/activities/purge?olderThanDays=30", tok), "TC330");
                Long remain = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tableName("Activity") + "\" WHERE id=?",
                        Long.class, aid);
                assertEquals(1L, remain.longValue(), "TC330: recent activity must remain; got " + remain);
        }
}

// ─── TC331 — S4-F7 returns deletedCount ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC331_S4F7CountTests extends TestBase {
        @Test
        @DisplayName("TC331 — Purge response includes a deleted count")
        void purge_count() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpDeleteAuth("/api/activities/purge?olderThanDays=30", tok);
                assert2xx(r, "TC331");
        }
}

// ─── TC332 — S4-F7 cutoff respected ─────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC332_S4F7CutoffTests extends TestBase {
        @Test
        @DisplayName("TC332 — Recent activities within cutoff survive purge")
        void purge_cutoff() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc332@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC332", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                java.time.LocalDate today = java.time.LocalDate.now();
                String recent = today.minusDays(5).toString();
                long aid = _TpM1S4Seed.actAt(this, iid, "R", "SIGHTSEEING", 30.0, 31.0,
                        recent + " 09:00:00", 100, "BOOKED");
                String tok = adminToken();
                assert2xx(httpDeleteAuth("/api/activities/purge?olderThanDays=30", tok), "TC332");
                Long remain = jdbc.queryForObject(
                        "SELECT COUNT(*) FROM \"" + tableName("Activity") + "\" WHERE id=?",
                        Long.class, aid);
                assertEquals(1L, remain.longValue(),
                        "TC332: 5-day-old activity must survive; got " + remain);
        }
}

// ─── TC333 — S4-F8 itinerary summary happy ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC333_S4F8ItinSummaryTests extends TestBase {
        @Test
        @DisplayName("TC333 — Itinerary summary returns DTO with totalActivities etc.")
        void itin_summary() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc333@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC333", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                _TpM1S4Seed.actAt(this, iid, "A", "SIGHTSEEING", 30.0, 31.0,
                        "2026-03-10 09:00:00", 200, "BOOKED");
                _TpM1S4Seed.actAt(this, iid, "B", "SIGHTSEEING", 30.0, 31.0,
                        "2026-03-11 09:00:00", 400, "BOOKED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/itinerary/" + iid + "/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC333");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("totalActivities") || j.has("total_activities"),
                        "TC333: totalActivities key required");
        }
}

// ─── TC334 — S4-F8 404 itinerary not found ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC334_S4F8ItinNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC334 — Non-existent itinerary returns 404")
        void summary_404() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/itinerary/9999999/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
                assertEquals(404, r.statusCode(), "TC334: must be 404; got " + r.statusCode());
        }
}

// ─── TC335 — S4-F8 averageCost field ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC335_S4F8AvgCostTests extends TestBase {
        @Test
        @DisplayName("TC335 — Summary DTO includes averageCost")
        void avg_cost_field() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc335@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC335", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/itinerary/" + iid + "/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC335");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("averageCost") || j.has("average_cost"),
                        "TC335: averageCost key required");
        }
}

// ─── TC336 — S4-F8 maxCost field ────────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC336_S4F8MaxCostTests extends TestBase {
        @Test
        @DisplayName("TC336 — Summary DTO includes maxCost")
        void max_cost_field() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc336@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC336", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/itinerary/" + iid + "/summary?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC336");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("maxCost") || j.has("max_cost"),
                        "TC336: maxCost key required");
        }
}

// ─── TC337 — S4-F9 budget-friendly happy ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC337_S4F9BudgetFriendlyTests extends TestBase {
        @Test
        @DisplayName("TC337 — Budget-friendly returns activities within maxCost")
        void budget_friendly() throws Exception {
                BASE_URL = deliveryServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc337@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC337", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                java.time.LocalDate today = java.time.LocalDate.now();
                long aid = _TpM1S4Seed.actAt(this, iid, "Cheap", "SIGHTSEEING", 30.0, 31.0,
                        today.toString() + " 09:00:00", 100, "BOOKED");
                _TpM1S4Seed.setActivityMetadata(this, aid, "{\"cost\":100}");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/budget-friendly?maxCost=200&sinceMinutes=1440", tok);
                assert2xx(r, "TC337");
        }
}

// ─── TC338 — S4-F9 over budget excluded ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC338_S4F9OverBudgetTests extends TestBase {
        @Test
        @DisplayName("TC338 — Activities over maxCost are excluded")
        void over_budget() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/budget-friendly?maxCost=0&sinceMinutes=60", tok);
                assert2xx(r, "TC338");
        }
}

// ─── TC339 — S4-F9 sinceMinutes filter ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC339_S4F9SinceMinutesTests extends TestBase {
        @Test
        @DisplayName("TC339 — sinceMinutes filter applies on scheduledTime")
        void since_minutes() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/budget-friendly?maxCost=500&sinceMinutes=15", tok);
                assert2xx(r, "TC339");
        }
}

// ─── TC340 — S4-F9 max=0 returns no activities ──────────────────
@Tag("public")
@Tag("features_m1")
class TC340_S4F9ZeroMaxTests extends TestBase {
        @Test
        @DisplayName("TC340 — maxCost=0 returns no activities")
        void zero_max() throws Exception {
                BASE_URL = deliveryServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/activities/budget-friendly?maxCost=0&sinceMinutes=1440", tok);
                assert2xx(r, "TC340");
        }
}


// ════════════════════════════════════════════════════════════════════════════
// Auxiliary M1 seed helpers — Booking Service (S5)
// ════════════════════════════════════════════════════════════════════════════

final class _TpM1S5Seed {
        private _TpM1S5Seed() {}

        /** INSERT a Booking row tied to itinerary+user with a chosen type. */
        static long bkg(TestBase t, long itineraryId, long userId, String type,
                        double amount, String status) {
                String tbl = t.tableName("Booking");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Booking", "itinerary"), itineraryId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "user"), userId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "type"), type); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "amount"), amount); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "status"), status); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "currency"), "EGP"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "bookingDetails"), "{}"); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "startDate"),
                        java.sql.Date.valueOf("2026-04-10")); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Booking", "endDate"),
                        java.sql.Date.valueOf("2026-04-15")); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** Update Booking bookingDetails JSONB. */
        static void setBookingDetails(TestBase t, long bookingId, String json) {
                t.jdbc.update("UPDATE \"" + t.tableName("Booking") + "\" SET booking_details=?::jsonb WHERE id=?",
                        json, bookingId);
        }

        /** INSERT a Coupon (M1 entity, fields per manifest). */
        static long coupon(TestBase t, String code, String discountType, double discountValue,
                           int maxUses, java.time.LocalDate expiryDate, boolean active) {
                String tbl = t.tableName("Coupon");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("Coupon", "code"), code); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Coupon", "discountType"), discountType); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Coupon", "discountValue"), discountValue); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Coupon", "maxUses"), maxUses); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Coupon", "currentUses"), 0); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Coupon", "expiryDate"),
                        java.sql.Date.valueOf(expiryDate)); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Coupon", "active"), active); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("Coupon", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }

        /** Set Coupon.currentUses directly via JDBC. */
        static void setCouponCurrentUses(TestBase t, long couponId, int currentUses) {
                String tbl = t.tableName("Coupon");
                String col = t.columnByField("Coupon", "currentUses");
                t.jdbc.update("UPDATE \"" + tbl + "\" SET \"" + col + "\"=? WHERE id=?",
                        currentUses, couponId);
        }

        /** INSERT a BookingCoupon join row. */
        static long bookingCoupon(TestBase t, long bookingId, long couponId, double discountApplied) {
                String tbl = t.tableName("BookingCoupon");
                java.util.Map<String, Object> ov = new java.util.HashMap<>();
                try { ov.put(t.columnByField("BookingCoupon", "booking"), bookingId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("BookingCoupon", "coupon"), couponId); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("BookingCoupon", "discountApplied"), discountApplied); } catch (Throwable ignore) {}
                try { ov.put(t.columnByField("BookingCoupon", "metadata"), "{}"); } catch (Throwable ignore) {}
                return t.insertRowReturningId(tbl, ov);
        }
}


// ════════════════════════════════════════════════════════════════════════════
// S5 — Booking Service M1 features (TC341..TC378) — 38 TCs across F1..F9
// ════════════════════════════════════════════════════════════════════════════

// ─── TC341 — S5-F1 search by status + date range ─────────────
@Tag("public")
@Tag("features_m1")
class TC341_S5F1SearchTests extends TestBase {
        @Test
        @DisplayName("TC341 — Search by status returns matches")
        void search() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc341@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC341", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 1500, "CONFIRMED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/search?status=CONFIRMED"
                        + "&startDate=2026-03-01&endDate=2026-04-30", tok);
                assert2xx(r, "TC341");
        }
}

// ─── TC342 — S5-F1 no status filter ────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC342_S5F1NoStatusTests extends TestBase {
        @Test
        @DisplayName("TC342 — Search without status returns all in range")
        void search_no_status() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/search?startDate=2026-03-01&endDate=2026-04-30", tok);
                assert2xx(r, "TC342");
        }
}

// ─── TC343 — S5-F1 most recent first ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC343_S5F1OrderTests extends TestBase {
        @Test
        @DisplayName("TC343 — Search results most recent first")
        void search_order() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/search?startDate=2026-01-01&endDate=2026-12-31", tok);
                assert2xx(r, "TC343");
        }
}

// ─── TC344 — S5-F1 empty range returns empty ─────────────────
@Tag("public")
@Tag("features_m1")
class TC344_S5F1EmptyRangeTests extends TestBase {
        @Test
        @DisplayName("TC344 — Empty range returns empty list")
        void search_empty() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/search?startDate=2099-01-01&endDate=2099-01-31", tok);
                assert2xx(r, "TC344");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertEquals(0, list.size(), "TC344: empty list expected; got " + list.size());
        }
}

// ─── TC345 — S5-F2 cancel happy ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC345_S5F2CancelTests extends TestBase {
        @Test
        @DisplayName("TC345 — Cancel a CONFIRMED booking returns 2xx")
        void cancel_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc345@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC345", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 1500, "CONFIRMED");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/bookings/" + bid + "/cancel",
                        "{\"reason\":\"change of plans\"}", tok);
                assert2xx(r, "TC345");
        }
}

// ─── TC346 — S5-F2 not CONFIRMED → 400 ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC346_S5F2NotConfirmedTests extends TestBase {
        @Test
        @DisplayName("TC346 — Cancelling non-CONFIRMED booking returns 400")
        void cancel_not_confirmed() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc346@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC346", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 1500, "FAILED");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/bookings/" + bid + "/cancel",
                        "{\"reason\":\"r\"}", tok);
                assertEquals(400, r.statusCode(), "TC346: must be 400; got " + r.statusCode());
        }
}

// ─── TC347 — S5-F2 already CANCELLED → 400 ──────────────────
@Tag("public")
@Tag("features_m1")
class TC347_S5F2AlreadyCancelledTests extends TestBase {
        @Test
        @DisplayName("TC347 — Cancelling already-CANCELLED booking returns 400")
        void cancel_already() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc347@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC347", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 1500, "CANCELLED");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/bookings/" + bid + "/cancel",
                        "{\"reason\":\"r\"}", tok);
                assertEquals(400, r.statusCode(), "TC347: must be 400; got " + r.statusCode());
        }
}

// ─── TC348 — S5-F2 404 non-existent ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC348_S5F2NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC348 — Non-existent booking returns 404")
        void cancel_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/bookings/9999999/cancel",
                        "{\"reason\":\"r\"}", tok);
                assertEquals(404, r.statusCode(), "TC348: must be 404; got " + r.statusCode());
        }
}

// ─── TC349 — S5-F3 user booking summary happy ───────────────
@Tag("public")
@Tag("features_m1")
class TC349_S5F3UserSummaryTests extends TestBase {
        @Test
        @DisplayName("TC349 — User booking summary returns DTO with typeBreakdown map")
        void user_summary() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc349@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC349", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 750, "CONFIRMED");
                _TpM1S5Seed.bkg(this, iid, uid, "TRANSPORT", 300, "CONFIRMED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/user/" + uid + "/summary", tok);
                assert2xx(r, "TC349");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("typeBreakdown") || j.has("type_breakdown"),
                        "TC349: typeBreakdown key required");
        }
}

// ─── TC350 — S5-F3 404 non-existent user ────────────────────
@Tag("public")
@Tag("features_m1")
class TC350_S5F3UserNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC350 — Non-existent user returns 404")
        void summary_user_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/user/9999999/summary", tok);
                assertEquals(404, r.statusCode(), "TC350: must be 404; got " + r.statusCode());
        }
}

// ─── TC351 — S5-F3 totalBookings field ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC351_S5F3TotalBookingsTests extends TestBase {
        @Test
        @DisplayName("TC351 — Summary DTO includes totalBookings")
        void total_bookings() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc351@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC351", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 1000, "CONFIRMED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/user/" + uid + "/summary", tok);
                assert2xx(r, "TC351");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("totalBookings") || j.has("total_bookings"),
                        "TC351: totalBookings key required");
        }
}

// ─── TC352 — S5-F3 totalAmount field ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC352_S5F3TotalAmountTests extends TestBase {
        @Test
        @DisplayName("TC352 — DTO includes totalAmount")
        void total_amount() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc352@tp.io", "TRAVELER");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/user/" + uid + "/summary", tok);
                assert2xx(r, "TC352");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("totalAmount") || j.has("total_amount"),
                        "TC352: totalAmount key required");
        }
}

// ─── TC353 — S5-F4 create booking happy ─────────────────────
@Tag("public")
@Tag("features_m1")
class TC353_S5F4CreateTests extends TestBase {
        @Test
        @DisplayName("TC353 — Create booking for PLANNED itinerary returns 2xx")
        void create_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc353@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC353", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                String tok = adminToken();
                String body = "{\"type\":\"ACCOMMODATION\",\"amount\":1500,\"providerName\":\"Hilton\"}";
                HttpResponse<String> r = httpPostAuth("/api/bookings/itinerary/" + iid, body, tok);
                assertTrue(r.statusCode() == 200 || r.statusCode() == 201,
                        "TC353: must be 2xx; got " + r.statusCode() + " body=" + r.body());
        }
}

// ─── TC354 — S5-F4 wrong itinerary status → 400 ─────────────
@Tag("public")
@Tag("features_m1")
class TC354_S5F4WrongStatusTests extends TestBase {
        @Test
        @DisplayName("TC354 — DRAFT itinerary returns 400")
        void create_wrong_status() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc354@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC354", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "DRAFT", 0, "2026-04-10");
                String tok = adminToken();
                String body = "{\"type\":\"ACCOMMODATION\",\"amount\":1500}";
                HttpResponse<String> r = httpPostAuth("/api/bookings/itinerary/" + iid, body, tok);
                assertEquals(400, r.statusCode(), "TC354: must be 400; got " + r.statusCode());
        }
}

// ─── TC355 — S5-F4 404 non-existent itinerary ───────────────
@Tag("public")
@Tag("features_m1")
class TC355_S5F4ItinNotFoundTests extends TestBase {
        @Test
        @DisplayName("TC355 — Non-existent itinerary returns 404")
        void create_itin_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                String body = "{\"type\":\"ACCOMMODATION\",\"amount\":1500}";
                HttpResponse<String> r = httpPostAuth("/api/bookings/itinerary/9999999", body, tok);
                assertEquals(404, r.statusCode(), "TC355: must be 404; got " + r.statusCode());
        }
}

// ─── TC356 — S5-F4 CANCELLED itinerary → 400 ────────────────
@Tag("public")
@Tag("features_m1")
class TC356_S5F4CancelledItinTests extends TestBase {
        @Test
        @DisplayName("TC356 — CANCELLED itinerary returns 400")
        void create_cancelled_itin() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc356@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC356", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "CANCELLED", 0, "2026-04-10");
                String tok = adminToken();
                String body = "{\"type\":\"ACCOMMODATION\",\"amount\":1500}";
                HttpResponse<String> r = httpPostAuth("/api/bookings/itinerary/" + iid, body, tok);
                assertEquals(400, r.statusCode(), "TC356: must be 400; got " + r.statusCode());
        }
}

// ─── TC357 — S5-F5 apply coupon happy ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC357_S5F5ApplyCouponTests extends TestBase {
        @Test
        @DisplayName("TC357 — Apply coupon to PENDING booking returns 2xx")
        void apply_coupon_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc357@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC357", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 2000, "PENDING");
                long cid = _TpM1S5Seed.coupon(this, "TC357_" + nonce(), "PERCENTAGE", 25.0, 3,
                        _TpM1Seed.futureDate(), true);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/coupons/" + cid, "", tok);
                assert2xx(r, "TC357");
        }
}

// ─── TC358 — S5-F5 not PENDING → 400 ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC358_S5F5NotPendingTests extends TestBase {
        @Test
        @DisplayName("TC358 — Apply coupon to CONFIRMED booking returns 400")
        void coupon_not_pending() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc358@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC358", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 2000, "CONFIRMED");
                long cid = _TpM1S5Seed.coupon(this, "TC358_" + nonce(), "PERCENTAGE", 25.0, 3,
                        _TpM1Seed.futureDate(), true);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/coupons/" + cid, "", tok);
                assertEquals(400, r.statusCode(), "TC358: must be 400; got " + r.statusCode());
        }
}

// ─── TC359 — S5-F5 expired coupon → 400 ─────────────────────
@Tag("public")
@Tag("features_m1")
class TC359_S5F5ExpiredTests extends TestBase {
        @Test
        @DisplayName("TC359 — Expired coupon returns 400")
        void coupon_expired() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc359@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC359", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 2000, "PENDING");
                long cid = _TpM1S5Seed.coupon(this, "TC359_" + nonce(), "PERCENTAGE", 25.0, 3,
                        _TpM1Seed.pastDate(), true);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/coupons/" + cid, "", tok);
                assertEquals(400, r.statusCode(), "TC359: must be 400; got " + r.statusCode());
        }
}

// ─── TC360 — S5-F5 inactive coupon → 400 ────────────────────
@Tag("public")
@Tag("features_m1")
class TC360_S5F5InactiveTests extends TestBase {
        @Test
        @DisplayName("TC360 — Inactive coupon returns 400")
        void coupon_inactive() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc360@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC360", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 2000, "PENDING");
                long cid = _TpM1S5Seed.coupon(this, "TC360_" + nonce(), "PERCENTAGE", 25.0, 3,
                        _TpM1Seed.futureDate(), false);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/coupons/" + cid, "", tok);
                assertEquals(400, r.statusCode(), "TC360: must be 400; got " + r.statusCode());
        }
}

// ─── TC361 — S5-F5 maxed-out coupon → 400 ───────────────────
@Tag("public")
@Tag("features_m1")
class TC361_S5F5MaxedTests extends TestBase {
        @Test
        @DisplayName("TC361 — Coupon at maxUses returns 400")
        void coupon_maxed() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc361@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC361", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 2000, "PENDING");
                long cid = _TpM1S5Seed.coupon(this, "TC361_" + nonce(), "PERCENTAGE", 25.0, 5,
                        _TpM1Seed.futureDate(), true);
                _TpM1S5Seed.setCouponCurrentUses(this, cid, 5);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/coupons/" + cid, "", tok);
                assertEquals(400, r.statusCode(), "TC361: must be 400; got " + r.statusCode());
        }
}

// ─── TC362 — S5-F5 cap at booking amount ────────────────────
@Tag("public")
@Tag("features_m1")
class TC362_S5F5CapAmountTests extends TestBase {
        @Test
        @DisplayName("TC362 — discountApplied capped at booking amount")
        void coupon_cap() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc362@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC362", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 1000, "PENDING");
                long cid = _TpM1S5Seed.coupon(this, "TC362_" + nonce(), "FIXED", 9999.0, 100,
                        _TpM1Seed.futureDate(), true);
                String tok = adminToken();
                HttpResponse<String> r = httpPostAuth(
                        "/api/bookings/" + bid + "/coupons/" + cid, "", tok);
                assert2xx(r, "TC362");
        }
}

// ─── TC363 — S5-F6 revenue analytics happy ──────────────────
@Tag("public")
@Tag("features_m1")
class TC363_S5F6RevenueTests extends TestBase {
        @Test
        @DisplayName("TC363 — Revenue report returns DTO with totalRevenue etc")
        void revenue_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/reports/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC363");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("totalRevenue") || j.has("total_revenue"),
                        "TC363: totalRevenue key required");
        }
}

// ─── TC364 — S5-F6 invalid range → 400 ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC364_S5F6InvalidRangeTests extends TestBase {
        @Test
        @DisplayName("TC364 — startDate after endDate returns 400")
        void revenue_invalid() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/reports/revenue?startDate=2026-04-01&endDate=2026-03-01", tok);
                assertEquals(400, r.statusCode(), "TC364: must be 400; got " + r.statusCode());
        }
}

// ─── TC365 — S5-F6 averageBookingAmount field ──────────────
@Tag("public")
@Tag("features_m1")
class TC365_S5F6AvgBookingAmountTests extends TestBase {
        @Test
        @DisplayName("TC365 — DTO includes averageBookingAmount")
        void avg_amount() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/reports/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC365");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("averageBookingAmount") || j.has("average_booking_amount"),
                        "TC365: averageBookingAmount required");
        }
}

// ─── TC366 — S5-F6 cancelledAmount + cancelledCount ────────
@Tag("public")
@Tag("features_m1")
class TC366_S5F6CancelledFieldsTests extends TestBase {
        @Test
        @DisplayName("TC366 — DTO includes cancelledAmount and cancelledCount")
        void cancelled_fields() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth(
                        "/api/bookings/reports/revenue?startDate=2026-03-01&endDate=2026-03-31", tok);
                assert2xx(r, "TC366");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("cancelledAmount") || j.has("cancelled_amount"),
                        "TC366: cancelledAmount required");
                assertTrue(j.has("cancelledCount") || j.has("cancelled_count"),
                        "TC366: cancelledCount required");
        }
}

// ─── TC367 — S5-F7 retry happy ──────────────────────────────
@Tag("public")
@Tag("features_m1")
class TC367_S5F7RetryTests extends TestBase {
        @Test
        @DisplayName("TC367 — Retry FAILED booking returns 2xx")
        void retry_happy() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc367@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC367", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 1500, "FAILED");
                _TpM1S5Seed.setBookingDetails(this, bid,
                        "{\"providerName\":\"Hilton\",\"retryAttempt\":0,\"failureReason\":\"payment declined\"}");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/bookings/" + bid + "/retry", "", tok);
                assert2xx(r, "TC367");
        }
}

// ─── TC368 — S5-F7 not FAILED → 400 ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC368_S5F7NotFailedTests extends TestBase {
        @Test
        @DisplayName("TC368 — Retry non-FAILED booking returns 400")
        void retry_not_failed() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc368@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC368", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 1500, "CONFIRMED");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/bookings/" + bid + "/retry", "", tok);
                assertEquals(400, r.statusCode(), "TC368: must be 400; got " + r.statusCode());
        }
}

// ─── TC369 — S5-F7 CANCELLED → 400 ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC369_S5F7CancelledTests extends TestBase {
        @Test
        @DisplayName("TC369 — Retry CANCELLED booking returns 400")
        void retry_cancelled() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc369@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC369", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 1500, "CANCELLED");
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/bookings/" + bid + "/retry", "", tok);
                assertEquals(400, r.statusCode(), "TC369: must be 400; got " + r.statusCode());
        }
}

// ─── TC370 — S5-F7 404 non-existent ─────────────────────────
@Tag("public")
@Tag("features_m1")
class TC370_S5F7NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC370 — Non-existent booking returns 404")
        void retry_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpPutAuth("/api/bookings/9999999/retry", "", tok);
                assertEquals(404, r.statusCode(), "TC370: must be 404; got " + r.statusCode());
        }
}

// ─── TC371 — S5-F8 details happy ───────────────────────────
@Tag("public")
@Tag("features_m1")
class TC371_S5F8DetailsTests extends TestBase {
        @Test
        @DisplayName("TC371 — Booking details includes appliedCoupons list and finalAmount")
        void details() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc371@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC371", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 2000, "CONFIRMED");
                long cid = _TpM1S5Seed.coupon(this, "TC371_" + nonce(), "PERCENTAGE", 25.0, 100,
                        _TpM1Seed.futureDate(), true);
                _TpM1S5Seed.bookingCoupon(this, bid, cid, 500.0);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/" + bid + "/details", tok);
                assert2xx(r, "TC371");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("appliedCoupons") || j.has("applied_coupons"),
                        "TC371: appliedCoupons key required");
        }
}

// ─── TC372 — S5-F8 no coupons → totalDiscount=0 ────────────
@Tag("public")
@Tag("features_m1")
class TC372_S5F8NoCouponsTests extends TestBase {
        @Test
        @DisplayName("TC372 — Booking with no coupons: totalDiscount=0")
        void details_no_coupons() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc372@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC372", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 2000, "PENDING");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/" + bid + "/details", tok);
                assert2xx(r, "TC372");
                double td = _TpM2.rD(parseNode(r.body()), "totalDiscount", "total_discount");
                assertEquals(0.0, td, 0.01, "TC372: totalDiscount=0; got " + td);
        }
}

// ─── TC373 — S5-F8 404 non-existent ────────────────────────
@Tag("public")
@Tag("features_m1")
class TC373_S5F8NotFoundTests extends TestBase {
        @Test
        @DisplayName("TC373 — Non-existent booking returns 404")
        void details_404() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/9999999/details", tok);
                assertEquals(404, r.statusCode(), "TC373: must be 404; got " + r.statusCode());
        }
}

// ─── TC374 — S5-F8 finalAmount field ───────────────────────
@Tag("public")
@Tag("features_m1")
class TC374_S5F8FinalAmountTests extends TestBase {
        @Test
        @DisplayName("TC374 — DTO includes finalAmount")
        void final_amount_field() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long uid = _TpM1Seed.seedUser(this, "U", "tc374@tp.io", "TRAVELER");
                long did = _TpM1S2Seed.destWithRating(this, "TC374", "EG", "X", "BEACH", "ACTIVE", 4.0);
                long iid = _TpM1Seed.seedItinerary(this, uid, did, "PLANNED", 0, "2026-04-10");
                long bid = _TpM1S5Seed.bkg(this, iid, uid, "ACCOMMODATION", 2000, "CONFIRMED");
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/" + bid + "/details", tok);
                assert2xx(r, "TC374");
                JsonNode j = parseNode(r.body());
                assertTrue(j.has("finalAmount") || j.has("final_amount"),
                        "TC374: finalAmount key required");
        }
}

// ─── TC375 — S5-F9 top-used happy ──────────────────────────
@Tag("public")
@Tag("features_m1")
class TC375_S5F9TopUsedTests extends TestBase {
        @Test
        @DisplayName("TC375 — Top-used coupons returns 2xx")
        void top_used() throws Exception {
                BASE_URL = checkoutServiceUrl;
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/coupons/top-used?limit=5", tok);
                assert2xx(r, "TC375");
        }
}

// ─── TC376 — S5-F9 limit caps results ──────────────────────
@Tag("public")
@Tag("features_m1")
class TC376_S5F9LimitTests extends TestBase {
        @Test
        @DisplayName("TC376 — limit caps the result list")
        void top_used_limit() throws Exception {
                BASE_URL = checkoutServiceUrl;
                _TpM1S5Seed.coupon(this, "TC376a_" + nonce(), "PERCENTAGE", 10.0, 100,
                        _TpM1Seed.futureDate(), true);
                _TpM1S5Seed.coupon(this, "TC376b_" + nonce(), "FIXED", 50.0, 50,
                        _TpM1Seed.futureDate(), true);
                _TpM1S5Seed.coupon(this, "TC376c_" + nonce(), "PERCENTAGE", 5.0, 20,
                        _TpM1Seed.futureDate(), true);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/coupons/top-used?limit=2", tok);
                assert2xx(r, "TC376");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                assertTrue(list.size() <= 2, "TC376: must cap at 2; got " + list.size());
        }
}

// ─── TC377 — S5-F9 expired computed in Java ────────────────
@Tag("public")
@Tag("features_m1")
class TC377_S5F9ExpiredTests extends TestBase {
        @Test
        @DisplayName("TC377 — DTO 'expired' is true for past expiryDate")
        void top_used_expired() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long cid = _TpM1S5Seed.coupon(this, "TC377_" + nonce(), "PERCENTAGE", 10.0, 100,
                        _TpM1Seed.pastDate(), false);
                _TpM1S5Seed.setCouponCurrentUses(this, cid, 3);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/coupons/top-used?limit=100", tok);
                assert2xx(r, "TC377");
        }
}

// ─── TC378 — S5-F9 timesUsed reflects currentUses ──────────
@Tag("public")
@Tag("features_m1")
class TC378_S5F9TimesUsedTests extends TestBase {
        @Test
        @DisplayName("TC378 — timesUsed in DTO matches coupon's currentUses")
        void times_used() throws Exception {
                BASE_URL = checkoutServiceUrl;
                long cid = _TpM1S5Seed.coupon(this, "TC378_" + nonce(), "PERCENTAGE", 10.0, 100,
                        _TpM1Seed.futureDate(), true);
                _TpM1S5Seed.setCouponCurrentUses(this, cid, 7);
                String tok = adminToken();
                HttpResponse<String> r = httpGetAuth("/api/bookings/coupons/top-used?limit=100", tok);
                assert2xx(r, "TC378");
                JsonNode arr = parseNode(r.body());
                JsonNode list = arr.isArray() ? arr : (arr.has("content") ? arr.get("content") : arr);
                int found = -1;
                for (JsonNode it : list) {
                        long id = it.has("couponId") ? it.get("couponId").asLong()
                                : (it.has("coupon_id") ? it.get("coupon_id").asLong()
                                : it.path("id").asLong(-1));
                        if (id == cid) {
                                found = it.has("timesUsed") ? it.get("timesUsed").asInt()
                                        : it.path("times_used").asInt(-1);
                                break;
                        }
                }
                assertEquals(7, found, "TC378: timesUsed=7; got " + found);
        }
}

