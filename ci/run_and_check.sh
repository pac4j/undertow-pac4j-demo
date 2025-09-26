#!/bin/bash

# Script to launch undertow-pac4j-demo and verify it works
# Usage: ./run_and_check.sh

set -e  # Stop script on error

echo "🚀 Starting undertow-pac4j-demo..."

# Go to project directory (one level up from ci/)
cd ..

# Clean and compile project
echo "📦 Compiling project..."
mvn clean package -q

# Ensure target directory exists
mkdir -p target

# Start Undertow in background
echo "🌐 Starting Undertow server..."
mvn exec:java > target/undertow.log 2>&1 &
UNDERTOW_PID=$!

# Wait for server to start (maximum 60 seconds)
echo "⏳ Waiting for server startup..."
for i in {1..60}; do
    if curl -s -o /dev/null -w "%{http_code}" http://localhost:8080 | grep -q "200"; then
        echo "✅ Server started successfully!"
        # Additional wait to ensure server is fully ready
        echo "⏳ Waiting additional 5 seconds for server to be fully ready..."
        sleep 5
        break
    fi
    if [ $i -eq 60 ]; then
        echo "❌ Timeout: Server did not start within 60 seconds"
        echo "📋 Server logs:"
        cat target/undertow.log
        kill $UNDERTOW_PID 2>/dev/null || true
        exit 1
    fi
    sleep 1
done

# Verify application responds correctly
echo "🔍 Verifying HTTP response..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" http://localhost:8080)

if [ "$HTTP_CODE" = "200" ]; then
    echo "✅ Application responds with HTTP 200"
    echo "🌐 Application accessible at: http://localhost:8080"
    
    # Test OAuth2/OIDC redirection
    echo "🔗 Testing OAuth2/OIDC redirection..."
    
    # Test OIDC client redirection
    OIDC_RESPONSE=$(curl -s -L -w "FINAL_URL:%{url_effective}\nHTTP_CODE:%{http_code}" "http://localhost:8080/oidc/index.html")
    OIDC_HTTP_CODE=$(echo "$OIDC_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
    OIDC_FINAL_URL=$(echo "$OIDC_RESPONSE" | grep "FINAL_URL:" | cut -d: -f2-)
    
    echo "🌐 OIDC Final URL: $OIDC_FINAL_URL"
    echo "📄 OIDC HTTP Code: $OIDC_HTTP_CODE"
    
    if [ "$OIDC_HTTP_CODE" = "200" ] || echo "$OIDC_FINAL_URL" | grep -q "accounts.google.com"; then
        echo "✅ OIDC redirection test passed!"
        OIDC_TEST_PASSED=true
    else
        echo "❌ OIDC redirection test failed!"
        OIDC_TEST_PASSED=false
    fi
    
    # Test CAS redirection
    echo "🔗 Testing CAS redirection..."
    
    CAS_RESPONSE=$(curl -s -L -w "FINAL_URL:%{url_effective}\nHTTP_CODE:%{http_code}" "http://localhost:8080/cas/index.html")
    CAS_HTTP_CODE=$(echo "$CAS_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
    CAS_FINAL_URL=$(echo "$CAS_RESPONSE" | grep "FINAL_URL:" | cut -d: -f2-)
    
    echo "🌐 CAS Final URL: $CAS_FINAL_URL"
    echo "📄 CAS HTTP Code: $CAS_HTTP_CODE"
    
    if [ "$CAS_HTTP_CODE" = "200" ] || echo "$CAS_FINAL_URL" | grep -q "casserverpac4j.herokuapp.com"; then
        echo "✅ CAS redirection test passed!"
        CAS_TEST_PASSED=true
    else
        echo "❌ CAS redirection test failed!"
        CAS_TEST_PASSED=false
    fi
    
    # Test Form authentication
    echo "🔗 Testing Form authentication..."
    
    FORM_RESPONSE=$(curl -s -L -w "FINAL_URL:%{url_effective}\nHTTP_CODE:%{http_code}" "http://localhost:8080/form/index.html")
    FORM_HTTP_CODE=$(echo "$FORM_RESPONSE" | grep "HTTP_CODE:" | cut -d: -f2)
    FORM_FINAL_URL=$(echo "$FORM_RESPONSE" | grep "FINAL_URL:" | cut -d: -f2-)
    
    echo "🌐 Form Final URL: $FORM_FINAL_URL"
    echo "📄 Form HTTP Code: $FORM_HTTP_CODE"
    
    if [ "$FORM_HTTP_CODE" = "200" ] || echo "$FORM_FINAL_URL" | grep -q "loginForm.html"; then
        echo "✅ Form authentication test passed!"
        FORM_TEST_PASSED=true
    else
        echo "❌ Form authentication test failed!"
        FORM_TEST_PASSED=false
    fi
    
else
    echo "❌ Initial test failed! HTTP code received: $HTTP_CODE"
    echo "📋 Server logs:"
    cat target/undertow.log
    OIDC_TEST_PASSED=false
    CAS_TEST_PASSED=false
    FORM_TEST_PASSED=false
fi

# Always stop the server
echo "🛑 Stopping server..."
kill $UNDERTOW_PID 2>/dev/null || true

# Wait a moment for graceful shutdown
sleep 2

# Force kill if still running
kill -9 $UNDERTOW_PID 2>/dev/null || true

if [ "$HTTP_CODE" = "200" ] && [ "$OIDC_TEST_PASSED" = "true" ] && [ "$CAS_TEST_PASSED" = "true" ] && [ "$FORM_TEST_PASSED" = "true" ]; then
    echo "🎉 undertow-pac4j-demo test completed successfully!"
    echo "✅ All tests passed:"
    echo "   - Application responds with HTTP 200"
    echo "   - OIDC redirection works correctly"
    echo "   - CAS redirection works correctly"
    echo "   - Form authentication works correctly"
    exit 0
else
    echo "💥 undertow-pac4j-demo test failed!"
    if [ "$HTTP_CODE" != "200" ]; then
        echo "❌ Application HTTP test failed (code: $HTTP_CODE)"
    fi
    if [ "$OIDC_TEST_PASSED" != "true" ]; then
        echo "❌ OIDC redirection test failed"
    fi
    if [ "$CAS_TEST_PASSED" != "true" ]; then
        echo "❌ CAS redirection test failed"
    fi
    if [ "$FORM_TEST_PASSED" != "true" ]; then
        echo "❌ Form authentication test failed"
    fi
    exit 1
fi