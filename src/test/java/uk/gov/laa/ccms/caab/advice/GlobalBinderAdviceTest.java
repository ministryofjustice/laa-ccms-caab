package uk.gov.laa.ccms.caab.advice;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.stereotype.Controller;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import uk.gov.laa.ccms.caab.bean.validators.BaselineTextValidator;

/**
 * Exercises the binder advice through a real request, which the rest of the suite does not - the
 * controller tests use {@code standaloneSetup} without registering controller advice, so they never
 * see it.
 */
class GlobalBinderAdviceTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TestController())
            .setControllerAdvice(new GlobalBinderAdvice(new BaselineTextValidator()))
            .build();
  }

  @Test
  @DisplayName("control characters are stripped even when the handler does not opt in")
  void controlCharactersAreStrippedWithoutValidated() throws Exception {
    final String withControlCharacters = "Sm" + (char) 0x00 + (char) 0x07 + "ith";

    mockMvc
        .perform(post("/unvalidated").param("name", withControlCharacters))
        .andExpect(status().isOk())
        .andExpect(content().string("Smith"));
  }

  @Test
  @DisplayName("tabs and newlines survive binding, because textareas need them")
  void whitespaceIsPreserved() throws Exception {
    mockMvc
        .perform(post("/unvalidated").param("name", "line one\nline two"))
        .andExpect(status().isOk())
        .andExpect(content().string("line one\nline two"));
  }

  @Test
  @DisplayName("angle brackets are rejected on a handler that opts in")
  void angleBracketsRejectedWhenValidated() throws Exception {
    mockMvc
        .perform(post("/validated").param("name", "<script>alert(1)</script>"))
        .andExpect(status().isOk())
        .andExpect(content().string("invalid.character"));
  }

  @Test
  @DisplayName("an ordinary value still binds cleanly on a handler that opts in")
  void ordinaryValuePassesValidation() throws Exception {
    mockMvc
        .perform(post("/validated").param("name", "O'Brien & Co."))
        .andExpect(status().isOk())
        .andExpect(content().string("ok"));
  }

  /**
   * Documents the boundary of the runtime net rather than asserting an ideal. Spring only runs
   * binder-registered validators for model attributes carrying {@code @Validated}, so a handler
   * without it gets control-character stripping but no angle-bracket check. This is the gap {@code
   * PostHandlerValidationGuardTest} stops from growing, and that {@code @Validated} rollout closes.
   * When every handler is annotated, this test should start failing and can be deleted.
   */
  @Test
  @DisplayName("known gap: a handler that does not opt in gets no angle-bracket check")
  void angleBracketsNotRejectedWithoutValidated() throws Exception {
    mockMvc
        .perform(post("/unvalidated").param("name", "<script>"))
        .andExpect(status().isOk())
        .andExpect(content().string("<script>"));
  }

  /** Plain accessors rather than Lombok: the annotation processor is main-source only. */
  public static class TestForm {
    private String name;

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }
  }

  @Controller
  static class TestController {

    @PostMapping("/unvalidated")
    @ResponseBody
    String unvalidated(@ModelAttribute final TestForm form) {
      return form.getName();
    }

    @PostMapping("/validated")
    @ResponseBody
    String validated(
        @Validated @ModelAttribute final TestForm form, final BindingResult bindingResult) {
      return bindingResult.hasErrors() ? bindingResult.getFieldError("name").getCode() : "ok";
    }
  }
}
