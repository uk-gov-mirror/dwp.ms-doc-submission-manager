package uk.gov.dwp.health.pip.document.submission.manager.model.application.fileuploads;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class FileUpload {

  private String sanitisedName;
  @Id
  private String id;
  private String displaySize;
  private Long size;
  private String s3Ref;
  private String bucket;
  private String mimetype;
  private String dateTime;
  private DrsDocTypeEnum drsDocType;

  public FileUpload(
      String sanitisedName, Long size, String s3Ref, String bucket, String mimetype,
      DrsDocTypeEnum drsDocType) {
    this.sanitisedName = sanitisedName;
    this.size = size;
    this.s3Ref = s3Ref;
    this.bucket = bucket;
    this.mimetype = mimetype;
    this.displaySize = getDisplaySize(size);
    this.dateTime = LocalDateTime.now().toString();
    this.drsDocType = drsDocType;
    this.id = new ObjectId().toString();
  }

  public String getDisplaySize(Long size) {
    if (size > 1000) {
      Long mbSize = size / 1000;
      return mbSize + " MB";
    }
    return size + " KB";
  }
}
