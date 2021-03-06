/** */
package mapper;

import com.fasterxml.jackson.databind.JsonNode;
import org.sunbird.common.models.util.LoggerEnum;
import org.sunbird.common.models.util.LoggerUtil;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.responsecode.ResponseCode;
import play.libs.Json;

/**
 * This class will map the requested json data into custom class.
 *
 * @author Manzarul
 */
public class RequestMapper {
  public static LoggerUtil logger = new LoggerUtil(RequestMapper.class);

  /**
   * Method to map request
   *
   * @param requestData JsonNode
   * @param obj Class<T>
   * @exception RuntimeException
   * @return <T>
   */
  public static <T> Object mapRequest(JsonNode requestData, Class<T> obj) throws RuntimeException {

    if (requestData == null)
      throw ProjectUtil.createClientException(ResponseCode.contentTypeRequiredError);

    try {
      return Json.fromJson(requestData, obj);
    } catch (Exception e) {
     logger.error(null, "ControllerRequestMapper error : " + e.getMessage(), e);
     logger.info(null,
          "RequestMapper:mapRequest Requested data : " + requestData);
      throw ProjectUtil.createClientException(ResponseCode.invalidData);
    }
  }
}
