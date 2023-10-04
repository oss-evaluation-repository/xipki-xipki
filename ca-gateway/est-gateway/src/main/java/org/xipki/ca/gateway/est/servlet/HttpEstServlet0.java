// Copyright (c) 2013-2023 xipki. All rights reserved.
// License Apache License 2.0

package org.xipki.ca.gateway.est.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xipki.audit.*;
import org.xipki.ca.gateway.GatewayUtil;
import org.xipki.ca.gateway.est.EstResponder;
import org.xipki.util.Args;
import org.xipki.util.IoUtil;
import org.xipki.util.LogUtil;
import org.xipki.util.http.HttpStatusCode;
import org.xipki.util.http.XiHttpRequest;
import org.xipki.util.http.XiHttpResponse;

import java.io.IOException;

/**
 * EST servlet.
 *
 * @author Lijun Liao (xipki)
 * @since 6.0.0
 */

public class HttpEstServlet0 {

  private static final Logger LOG = LoggerFactory.getLogger(HttpEstServlet0.class);

  private boolean logReqResp;

  private EstResponder responder;

  public void setLogReqResp(boolean logReqResp) {
    this.logReqResp = logReqResp;
  }

  public void setResponder(EstResponder responder) {
    this.responder = Args.notNull(responder, "responder");
  }

  public XiHttpResponse doGet(XiHttpRequest req) throws IOException {
    return service0(req, false);
  }

  public XiHttpResponse doPost(XiHttpRequest req) throws IOException {
    return service0(req, true);
  }

  private XiHttpResponse service0(XiHttpRequest req, boolean viaPost) throws IOException {
    AuditService auditService = Audits.getAuditService();
    AuditEvent event = new AuditEvent();
    event.setApplicationName("est-gw");

    byte[] requestBytes = null;
    XiHttpResponse restResp = null;
    try {
      String path = req.getServletPath();
      requestBytes = viaPost ? IoUtil.readAllBytes(req.getInputStream()) : null;
      restResp = responder.service(path, requestBytes, req, event);
      if (event.getStatus() == null) {
        event.setStatus(AuditStatus.SUCCESSFUL);
      }
      return restResp;
    } catch (RuntimeException ex) {
      event.setStatus(AuditStatus.FAILED);
      event.setLevel(AuditLevel.ERROR);
      LOG.error("RuntimeException thrown, this should not happen!", ex);
      return new XiHttpResponse(HttpStatusCode.SC_INTERNAL_SERVER_ERROR);
    } finally {
      LogUtil.logReqResp("EST Gateway", LOG, logReqResp, viaPost, req.getRequestURI(),
          requestBytes, restResp == null ? null : restResp.getBody());

      event.finish();
      auditService.logEvent(event);
      GatewayUtil.logAuditEvent(LOG, event);
    }
  } // method service0

}
