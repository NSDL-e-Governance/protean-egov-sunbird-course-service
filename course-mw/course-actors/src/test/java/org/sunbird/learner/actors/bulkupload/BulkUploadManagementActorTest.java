package org.sunbird.learner.actors.bulkupload;

import static akka.testkit.JavaTestKit.duration;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.Props;
import akka.testkit.javadsl.TestKit;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.*;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PowerMockIgnore;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.sunbird.cassandraimpl.CassandraOperationImpl;
import org.sunbird.common.exception.ProjectCommonException;
import org.sunbird.common.models.response.Response;
import org.sunbird.common.models.util.ActorOperations;
import org.sunbird.common.models.util.JsonKey;
import org.sunbird.common.models.util.ProjectLogger;
import org.sunbird.common.models.util.ProjectUtil;
import org.sunbird.common.request.Request;
import org.sunbird.common.request.RequestContext;
import org.sunbird.helper.ServiceFactory;
import org.sunbird.learner.util.Util;

/** @author arvind. Junit test cases for bulk upload - user, org, batch. */
@RunWith(PowerMockRunner.class)
@PrepareForTest({ServiceFactory.class, Util.class})
@PowerMockIgnore("javax.management.*")
public class BulkUploadManagementActorTest {

  private static ActorSystem system;
  private static final Props props = Props.create(BulkUploadManagementActor.class);
  private static final String USER_ID = "bcic783gfu239";
  private static final String refOrgId = "id34fy";
  private static CassandraOperationImpl cassandraOperation;
  private static final String PROCESS_ID = "process-13647-fuzzy";

  @BeforeClass
  public static void setUp() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
    system = ActorSystem.create("system");
    PowerMockito.mockStatic(Util.class);
  }

  @Before
  public void beforeEachTest() {
    PowerMockito.mockStatic(ServiceFactory.class);
    cassandraOperation = mock(CassandraOperationImpl.class);
    when(ServiceFactory.getInstance()).thenReturn(cassandraOperation);
  }

  @Test
  public void testBulkUploadGetStatus() {
    Response response = getCassandraRecordByIdForBulkUploadResponse();
    when(cassandraOperation.getRecordByIdentifier(Mockito.any(), Mockito.any(), Mockito.any(), Mockito.anyString(), Mockito.anyList()))
        .thenReturn(response);
    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);
    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.GET_BULK_OP_STATUS.getValue());
    reqObj.getRequest().put(JsonKey.PROCESS_ID, PROCESS_ID);
    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("10 second"), Response.class);
    List<Map<String, Object>> list = (List<Map<String, Object>>) res.get(JsonKey.RESPONSE);
    if (!list.isEmpty()) {
      Map<String, Object> map = list.get(0);
      String processId = (String) map.get(JsonKey.PROCESS_ID);
      Assert.assertTrue(null != processId);
    }
  }

  @Test
  public void testBatchBulkUploadCreateBatchSuccess() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    String headerLine = "batchId,userIds";
    String firstLine = "batch78575ir8478,\"bcic783gfu239,nhhuc37i5t8,h7884f7t8\"";
    StringBuilder builder = new StringBuilder();
    builder.append(headerLine).append("\n").append(firstLine);

    Response insertResponse = createCassandraInsertSuccessResponse();
    when(cassandraOperation.insertRecord(
            Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(insertResponse);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();

    innerMap.put(JsonKey.CREATED_BY, USER_ID);
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.BATCH_LEARNER_ENROL);

    innerMap.put(JsonKey.FILE, builder.toString().getBytes());
    reqObj.getRequest().put(JsonKey.DATA, innerMap);

    subject.tell(reqObj, probe.getRef());
    Response res = probe.expectMsgClass(duration("300 second"), Response.class);
    String processId = (String) res.get(JsonKey.PROCESS_ID);
    Assert.assertTrue(null != processId);
  }

  @Test
  public void testBatchBulkUploadWithInvalidFileHeaders() {

    TestKit probe = new TestKit(system);
    ActorRef subject = system.actorOf(props);

    String headerLine = "batchId,userIds,orgId";
    String firstLine = "batch78575ir8478,\"bcic783gfu239,nhhuc37i5t8,h7884f7t8\",org123";
    StringBuilder builder = new StringBuilder();
    builder.append(headerLine).append("\n").append(firstLine);

    Response insertResponse = createCassandraInsertSuccessResponse();
    when(cassandraOperation.insertRecord(
            Mockito.any(), Mockito.anyString(), Mockito.anyString(), Mockito.anyMap()))
        .thenReturn(insertResponse);

    Request reqObj = new Request();
    reqObj.setOperation(ActorOperations.BULK_UPLOAD.getValue());
    HashMap<String, Object> innerMap = new HashMap<>();

    innerMap.put(JsonKey.CREATED_BY, USER_ID);
    innerMap.put(JsonKey.OBJECT_TYPE, JsonKey.BATCH_LEARNER_ENROL);

    innerMap.put(JsonKey.FILE, builder.toString().getBytes());
    reqObj.getRequest().put(JsonKey.DATA, innerMap);

    subject.tell(reqObj, probe.getRef());
    ProjectCommonException res =
        probe.expectMsgClass(duration("10 second"), ProjectCommonException.class);
    Assert.assertTrue(null != res);
  }

  private Response createCassandraInsertSuccessResponse() {
    Response response = new Response();
    response.put(JsonKey.RESPONSE, JsonKey.SUCCESS);
    return response;
  }

  private Response getCassandraRecordByIdForBulkUploadResponse() {
    Response response = new Response();
    List<Map<String, Object>> list = new ArrayList<>();
    Map<String, Object> bulkUploadProcessMap = new HashMap<>();
    bulkUploadProcessMap.put(JsonKey.ID, "123");
    bulkUploadProcessMap.put(JsonKey.STATUS, ProjectUtil.BulkProcessStatus.COMPLETED.getValue());
    bulkUploadProcessMap.put(JsonKey.OBJECT_TYPE, JsonKey.ORGANISATION);
    list.add(bulkUploadProcessMap);
    response.put(JsonKey.RESPONSE, list);
    return response;
  }

  private byte[] getFileAsBytes(String fileName) {
    File file = null;
    byte[] bytes = null;
    try {
      file =
          new File(
              BulkUploadManagementActorTest.class.getClassLoader().getResource(fileName).getFile());
      Path path = Paths.get(file.getPath());
      bytes = Files.readAllBytes(path);
    } catch (FileNotFoundException e) {
      ProjectLogger.log(e.getMessage(), e);
    } catch (IOException e) {
      ProjectLogger.log(e.getMessage(), e);
    }
    return bytes;
  }
}
