package van.project.wechatter.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import static van.project.wechatter.util.MarkdownUtil.decodeAndRender;

@Slf4j
@Controller
@RequestMapping("/markdown")
public class MarkdownController {

    @GetMapping("/view")
    public String view(@RequestParam("data") String data, Model model) {
        // 调用共用的解码渲染方法
        String htmlBody = decodeAndRender(data);
        model.addAttribute("htmlContent", htmlBody);
        return "markdown-view"; // 对应 templates/markdown-view.html
    }

}
