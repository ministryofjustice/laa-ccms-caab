package uk.gov.laa.ccms.caab.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Component;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindingResult;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.ModelAndView;
import uk.gov.laa.ccms.caab.bean.validators.request.ProviderRequestDocumentUploadValidator;

/** Adds the file-size error after redirecting an over-limit multipart request to its form. */
@Component
public class UploadSizeErrorInterceptor implements HandlerInterceptor {

  private final ProviderRequestDocumentUploadValidator fileUploadValidator;

  public UploadSizeErrorInterceptor(ProviderRequestDocumentUploadValidator fileUploadValidator) {
    this.fileUploadValidator = fileUploadValidator;
  }

  @Override
  public void postHandle(
      HttpServletRequest request,
      HttpServletResponse response,
      Object handler,
      ModelAndView modelAndView) {
    HttpSession session = request.getSession(false);
    if (session == null || modelAndView == null || !"GET".equals(request.getMethod())) {
      return;
    }
    String path = request.getRequestURI().substring(request.getContextPath().length());
    if (!path.equals(session.getAttribute(UploadSizeErrorFilter.SESSION_ATTRIBUTE))) {
      return;
    }

    String formName = UploadSizeErrorRoutes.formName(path);
    if (formName == null) {
      return;
    }
    Object form = modelAndView.getModel().get(formName);
    if (form == null) {
      return;
    }
    session.removeAttribute(UploadSizeErrorFilter.SESSION_ATTRIBUTE);
    BindingResult errors = new BeanPropertyBindingResult(form, formName);
    fileUploadValidator.rejectFileSize(errors);
    modelAndView.getModel().put(BindingResult.MODEL_KEY_PREFIX + formName, errors);
  }
}
