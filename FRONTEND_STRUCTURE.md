+# Auto Mart Frontend Structure

The authentication UI and shared layout are now separated from Java string blocks so they are easier to edit.

## HTML templates
- `src/main/resources/templates/layout/base.html`
- `src/main/resources/templates/partials/header.html`
- `src/main/resources/templates/partials/footer.html`
- `src/main/resources/templates/auth/signin.html`
- `src/main/resources/templates/auth/signup.html`
- `src/main/resources/templates/auth/admin-login.html`

## Styling and interaction
- `src/main/resources/static/css/auth-premium.css`
- `src/main/resources/static/js/auth-premium.js`

## Rendering class
- `src/main/java/com/automart/TemplateRenderer.java`

## Editing tips
- Update the visual layout in the HTML template files.
- Change colors, spacing, and premium effects in `auth-premium.css`.
- Change password toggle, role toggle, and Gmail signup behavior in `auth-premium.js`.
- Shared site chrome lives in `header.html`, `footer.html`, and `base.html`.
