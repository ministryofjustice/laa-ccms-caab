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
            .setControllerAdvice(new GlobalBinderAdvice())
            .build();
  }

  @Test
  @DisplayName("angle brackets are rejected without the handler opting in")
  void angleBracketsRejectedWithoutValidated() throws Exception {
    mockMvc
        .perform(post("/plain").param("name", "<script>alert(1)</script>"))
        .andExpect(status().isOk())
        .andExpect(content().string("rejected"));
  }

  @Test
  @DisplayName("angle brackets are rejected on a handler that does opt in")
  void angleBracketsRejectedWhenValidated() throws Exception {
    mockMvc
        .perform(post("/validated").param("name", "<script>alert(1)</script>"))
        .andExpect(status().isOk())
        .andExpect(content().string("rejected"));
  }

  /**
   * The field that is not posted cannot be rejected, which is what makes this safe on the form
   * beans that are carried across several pages of a journey.
   */
  @Test
  @DisplayName("only submitted fields are checked")
  void onlySubmittedFieldsAreChecked() throws Exception {
    mockMvc
        .perform(post("/plain").param("name", "fine"))
        .andExpect(status().isOk())
        .andExpect(content().string("fine"));
  }

  @Test
  @DisplayName("control characters are stripped rather than rejected")
  void controlCharactersAreStripped() throws Exception {
    mockMvc
        .perform(post("/plain").param("name", "Sm" + (char) 0x00 + (char) 0x07 + "ith"))
        .andExpect(status().isOk())
        .andExpect(content().string("Smith"));
  }

  @Test
  @DisplayName("tabs and newlines survive binding, because textareas need them")
  void whitespaceIsPreserved() throws Exception {
    mockMvc
        .perform(post("/plain").param("name", "line one\nline two"))
        .andExpect(status().isOk())
        .andExpect(content().string("line one\nline two"));
  }

  /**
   * A rejected bind needs somewhere to report to. A handler taking a form bean without a
   * BindingResult gets a 400 rather than a re-rendered form, so one accepting typed input should
   * always declare one.
   */
  @Test
  @DisplayName("a handler with no BindingResult gets a 400 rather than a field error")
  void noBindingResultGivesBadRequest() throws Exception {
    mockMvc
        .perform(post("/no-binding-result").param("name", "<script>"))
        .andExpect(status().isBadRequest());
  }

  /** Plain accessors rather than Lombok: the annotation processor is main-source only. */
  public static class TestForm {
    private String name;
    private String other;

    public String getName() {
      return name;
    }

    public void setName(final String name) {
      this.name = name;
    }

    public String getOther() {
      return other;
    }

    public void setOther(final String other) {
      this.other = other;
    }
  }

  @Controller
  static class TestController {

    @PostMapping("/plain")
    @ResponseBody
    String plain(@ModelAttribute final TestForm form, final BindingResult bindingResult) {
      return bindingResult.hasErrors() ? "rejected" : form.getName();
    }

    @PostMapping("/validated")
    @ResponseBody
    String validated(
        @Validated @ModelAttribute final TestForm form, final BindingResult bindingResult) {
      return bindingResult.hasErrors() ? "rejected" : form.getName();
    }

    @PostMapping("/no-binding-result")
    @ResponseBody
    String noBindingResult(@ModelAttribute final TestForm form) {
      return form.getName();
    }
  }
}
