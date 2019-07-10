Feature: OnTour -> MSD (booking enrichment)

  Scenario Outline: File is successfully processed on MSD system throw proxy mode in ids-msd-manage-booking microservice
    Given booking <FILE> was uploaded to SFTP
    When incoming enriched booking with <BOOKING_NUMBER> has been sent to MSD system
    And execute /request/kibana.json for <BOOKING_NUMBER>
    Then response contains message Message processed successfully.


    Examples:
      | FILE         | BOOKING_NUMBER |
      | /tcv.xml.zip | 77336585       |
      | /sbw.xml.zip | 77336583       |
      | /uns.xml.zip | 77336586       |
      | /sfw.xml.zip | 77336584       |


