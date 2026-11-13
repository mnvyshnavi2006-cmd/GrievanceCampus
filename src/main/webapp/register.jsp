<%@ page contentType="text/html;charset=UTF-8" %>
<!doctype html>
<html lang="en">
<head>
  <meta charset="utf-8">
  <meta name="viewport" content="width=device-width, initial-scale=1">
  <title>Student registration · Grievance Campus</title>
  <link rel="stylesheet" href="assets/css/style.css">
  <link rel="stylesheet" href="assets/css/auth.css">
</head>
<body class="auth-page">
  <main class="auth-card">
    <a class="brand" href="index.jsp"><span class="brand-mark">G</span><span>Grievance Campus<small>Student support portal</small></span></a>
    <h1>Create student account</h1>
    <p class="muted">Register to submit grievances and track their progress.</p>
    <% String error = request.getParameter("error");
       if ("duplicate".equals(error)) { %>
      <div class="alert error" role="alert">An account with that email already exists. Sign in or use another email.</div>
    <% } else if (error != null) { %>
      <div class="alert error" role="alert">Registration could not be completed. Check the fields and try again.</div>
    <% } %>
    <form method="post" action="register" autocomplete="on">
      <label>Full name<input name="name" maxlength="120" autocomplete="name" required></label>
      <label>Email address<input type="email" name="email" maxlength="254" autocomplete="email" required></label>
      <label>Password <span class="muted small">(8–255 characters)</span><input type="password" name="password" minlength="8" maxlength="255" autocomplete="new-password" required></label>
      <label>Department<input name="department" maxlength="120" required></label>
      <label>Section<input name="section" maxlength="50" required></label>
      <button class="button primary full" type="submit">Register</button>
    </form>
    <p class="small muted">Already registered? <a href="login.jsp">Sign in</a></p>
  </main>
</body>
</html>
