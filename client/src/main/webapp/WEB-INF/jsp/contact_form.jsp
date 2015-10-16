<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<div id="contact-us-div">

    <form id="contact_form" action="${pageContext.request.contextPath}/contact" method="post" class="contact-form">
        <h4>Please report to us and we will get back shortly.</h4>
        <input type="hidden" name="source" id="source" value="${param.source}"/>
        <input type="hidden" name="exception" id="exception" value="${exception}"/>
        <input type="hidden" name="url" id="url" value="${url}"/>

        <div class="field">
            <label for="mailAddress"><h6>From:</h6></label>
            <input type="email" id="mailAddress" name="mailAddress" size="80" class="search"/>
        </div>
        <div class="field">
            <label for="to"><h6>To:</h6></label>
            <input type="to" id="to" name="to" size="80" class="search" value="Reactome Helpdesk" readonly>
        </div>
        <div class="field">
            <label for="subject"><h6>Subject:</h6></label>
            <input type="text" id="subject" name="subject" class="search" value="${subject}" readonly/>
        </div>
        <div class="fieldarea">
            <label for="message"><h6>Message:</h6></label>
                <textarea id="message" name="message" class="search" rows="5" cols="80"
                          width="70%">${message}</textarea>
        </div>
        <div style="width: 43%; text-align:right">
            <p>
                <input type="button" class="submit" value="Send" id="send"/>
            </p>
        </div>
    </form>
    <p>
        <span id="msg"/></p>

</div>
<script type="text/javascript">
    $(document).ready(function () {

        $('#send').click(function () {
            $('#send').prop("disabled", true);
            var formData = $("#contact_form");
            $.ajax({
                url: formData.attr("action"),
                type: "POST",
                data: formData.serialize(),
                success: function (data, textStatus, jqXHR) {
                    formData.remove();

                    $("#msg").replaceWith("<span id='msg'><h5>Thank you for contacting us.&nbsp;We will get back to you shortly.</h5></span>");
                    $("#msg").addClass("contact-msg-success");
                },
                error: function (jqXHR, textStatus, errorThrown) {
                    $('#send').prop("disabled", false);
                    $("#msg").replaceWith("<span id='msg'>Could not send your email. Try again or Please email us at <a href='mailto:help@reactome.org'>help@reactome.org</a></span>");
                    $("#msg").addClass("contact-msg-error");
                }
            });

        });
    });

</script>
