/*
================================================================================
LEGACY REFERENCE: LabDb - Schema Only
================================================================================
Database: LabDb (SQL Server)
Purpose: Reference for lab/diagnostic requirements. Use to extract table structures,
         views, and workflows. NOT for direct migration.
See: LEGACY-SQL-REVIEW-AND-REQUIREMENTS-PLAN.md

Removed: CREATE DATABASE, ALTER DATABASE, CREATE USER, ALTER ROLE, file paths
================================================================================
*/

USE [LabDb]
GO

/****** Object:  Table [dbo].[tb_InvMaster]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_InvMaster](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[InvNo] [nvarchar](50) NOT NULL,
	[InvDate] [date] NOT NULL,
	[PatientName] [nvarchar](1500) NOT NULL,
	[Age] [nvarchar](50) NOT NULL,
	[Sex] [nvarchar](50) NOT NULL,
	[MobileNo] [nvarchar](50) NOT NULL,
	[DrCode] [nvarchar](50) NOT NULL,
	[DrName] [nvarchar](1500) NOT NULL,
	[BedNo] [nvarchar](500) NOT NULL,
	[PtStatus] [nvarchar](50) NOT NULL,
	[PtYear] [nvarchar](50) NOT NULL,
	[EntryTime] [nvarchar](50) NOT NULL,
	[InvTime] [nvarchar](50) NULL,
	[PatientId] [nvarchar](50) NULL,
	[BranchId] [int] NOT NULL,
 CONSTRAINT [PK_tb_InvMaster] PRIMARY KEY CLUSTERED 
(
	[InvNo] ASC,
	[InvDate] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_InvDetails]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_InvDetails](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[MasterId] [int] NOT NULL,
	[Code] [nvarchar](50) NOT NULL,
	[ShortDesc] [nvarchar](1500) NOT NULL,
	[GroupName] [nvarchar](1500) NOT NULL,
	[VaqName] [nvarchar](1500) NOT NULL,
	[LabNo] [nvarchar](50) NOT NULL,
	[PtYear] [nvarchar](50) NOT NULL,
	[PrintNo] [int] NOT NULL,
	[IsSaved] [int] NOT NULL,
	[Valid] [int] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  View [dbo].[VW_LABTESTMAPPING]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[VW_LABTESTMAPPING] as 

select a.InvDate, a.InvNo,b.LabNo,a.PatientName,a.Sex,a.drCode,a.DrName,b.Code AS Pcode,b.ShortDesc,a.Age,a.BedNo
From tb_InvMaster a,tb_InvDetails b
Where a.Id=b.MasterId
GO
/****** Object:  Table [dbo].[Channeldefination]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Channeldefination](
	[SerialNo] [int] NULL,
	[Pcode] [nvarchar](50) NULL,
	[ShortDesc] [nvarchar](50) NULL,
	[Parameter] [nvarchar](50) NULL,
	[AliasName] [nvarchar](50) NULL,
	[NormalValue] [nvarchar](50) NULL,
	[Unit] [nvarchar](50) NULL,
	[Result] [nvarchar](50) NULL,
	[ReportingGroup] [nvarchar](50) NULL,
	[WillShow] [int] NULL,
	[InvNo] [nvarchar](50) NULL,
	[InvDate] [smalldatetime] NULL,
	[SignatureDrCode] [nvarchar](50) NULL,
	[SignatureDrName] [nvarchar](50) NULL,
	[Designation] [nvarchar](50) NULL,
	[SampleType] [nvarchar](50) NULL,
	[DeptName] [nvarchar](50) NULL,
	[MultiGroupName] [nvarchar](50) NULL,
	[MultiLineResult] [nvarchar](50) NULL,
	[ParameterType] [nvarchar](50) NULL,
	[Specimen] [nvarchar](50) NULL,
	[IsBold] [int] NULL,
	[MultipleValue] [int] NULL,
	[MachineName] [nvarchar](50) NULL,
	[IsResulted] [int] NULL,
	[GroupName] [nvarchar](50) NULL,
	[PostingDateTime] [datetime] NULL,
	[MachineCode] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  View [dbo].[V_VitrosChannelMapping]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[V_VitrosChannelMapping]
AS
select DISTINCT  a.LabNo,  b.Parameter, b.AliasName,  b.MachineName,a.PatientName,a.Sex,a.InvNo
from VW_LABTESTMAPPING a
left join Channeldefination b on b.Pcode = a.Pcode
where b.IsResulted = 1 AND b.MachineName IN ('VITROSECI','VITROS350')
GO
/****** Object:  Table [dbo].[tb_Parameter_Definition]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_Parameter_Definition](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[TestCode] [nvarchar](250) NOT NULL,
	[TestName] [nvarchar](500) NOT NULL,
	[Specimen] [nvarchar](500) NOT NULL,
	[MachineName] [nvarchar](150) NULL,
	[ReportHeaderName] [nvarchar](500) NULL,
	[GroupSlNo] [int] NOT NULL,
	[ParameterSlNo] [int] NOT NULL,
	[Parameter] [nvarchar](500) NOT NULL,
	[Alias] [nvarchar](500) NOT NULL,
	[NormalValue] [nvarchar](1000) NULL,
	[Unit] [nvarchar](500) NULL,
	[Result] [nvarchar](1050) NULL,
	[ReportingGroup] [nvarchar](500) NULL,
	[IsBold] [int] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_MachineDataMaster]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_MachineDataMaster](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[MasterId] [int] NOT NULL,
	[InvNo] [nvarchar](50) NOT NULL,
	[InvDate] [date] NOT NULL,
	[ReportNo] [nvarchar](50) NOT NULL,
	[CheckedByName] [nvarchar](500) NULL,
	[CheckedByDegree] [nvarchar](500) NULL,
	[ConsultantName] [nvarchar](500) NULL,
	[ConsultantDegree] [nvarchar](500) NULL,
	[LabInchargeName] [nvarchar](500) NULL,
	[LabInchargeDegree] [nvarchar](500) NULL,
	[Comments] [nvarchar](500) NULL,
	[TestNameReport] [nvarchar](1500) NULL,
	[Organism] [nvarchar](1500) NOT NULL,
	[ColonyCount] [nvarchar](1500) NOT NULL,
	[Incubation] [nvarchar](1500) NOT NULL,
	[SpecificTest] [nvarchar](1500) NOT NULL,
	[PCode] [nvarchar](50) NOT NULL,
	[TestCode] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_LabSampleStatusInfo]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_LabSampleStatusInfo](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[MasterId] [int] NOT NULL,
	[InvNo] [nvarchar](50) NOT NULL,
	[SampleNo] [nvarchar](50) NOT NULL,
	[ReportNo] [nvarchar](50) NOT NULL,
	[TestCode] [nvarchar](50) NOT NULL,
	[CollStatus] [nvarchar](10) NOT NULL,
	[CollTime] [datetime] NULL,
	[CollUser] [nvarchar](15) NOT NULL,
	[SendStatus] [nvarchar](50) NOT NULL,
	[SendTime] [datetime] NULL,
	[SendUser] [nvarchar](50) NOT NULL,
	[ReceiveInLabStatus] [nvarchar](10) NOT NULL,
	[ReceiveInLabTime] [datetime] NULL,
	[ReceiveInLabUser] [nvarchar](50) NOT NULL,
	[ReportPrintStatus] [nvarchar](10) NOT NULL,
	[ReportPrintTime] [datetime] NULL,
	[ReportPrintUser] [nvarchar](50) NOT NULL,
	[ReportProcessStatus] [nvarchar](10) NOT NULL,
	[ReportProcessTime] [datetime] NULL,
	[ReportProcessUser] [nvarchar](50) NOT NULL,
	[RReceiveInDelCounterStatus] [nvarchar](10) NOT NULL,
	[RReceiveInDelCounterTime] [datetime] NULL,
	[RReceiveInDelCounterUser] [nvarchar](50) NOT NULL,
	[DeliverToPatientStatus] [nvarchar](10) NOT NULL,
	[DeliverToPatientTime] [datetime] NULL,
	[DeliverToPatientUser] [nvarchar](50) NOT NULL,
	[RDeliveryDate] [date] NULL,
	[VaqGroup] [nvarchar](1500) NOT NULL,
	[VaqName] [nvarchar](1500) NOT NULL,
	[PYEAR] [numeric](18, 0) NOT NULL,
	[EntryDate] [datetime] NOT NULL,
	[HOSTNAME] [nvarchar](500) NOT NULL,
	[FinalStatus] [nvarchar](50) NULL,
	[BranchId] [int] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_MachineDataDtls]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_MachineDataDtls](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[MasterId] [int] NOT NULL,
	[InvNo] [nvarchar](50) NULL,
	[InvDate] [date] NOT NULL,
	[LabNo] [nvarchar](50) NULL,
	[ReportNo] [nvarchar](50) NULL,
	[TestCode] [nvarchar](50) NULL,
	[Parameter] [nvarchar](50) NOT NULL,
	[AliasNo] [nvarchar](50) NULL,
	[Result] [nvarchar](500) NULL,
	[Unit] [nvarchar](50) NULL,
	[NormalValue] [nvarchar](500) NULL,
	[ReportingGroupName] [nvarchar](100) NULL,
	[GroupSlNo] [int] NOT NULL,
	[SerialNo] [int] NOT NULL,
	[UserName] [nvarchar](50) NULL,
	[EntryDate] [date] NOT NULL,
	[MachineName] [nvarchar](250) NULL,
	[ReportHeaderName] [nvarchar](500) NULL,
	[IsBold] [int] NOT NULL,
	[IsPrint] [int] NOT NULL,
	[ReportFileName] [nvarchar](50) NULL,
	[Comments] [nvarchar](3500) NULL,
	[ManualSampleNo] [nvarchar](100) NOT NULL
) ON [PRIMARY]
GO
/****** Object:  View [dbo].[VW_GET_LAB_REPORT_VIEW]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[VW_GET_LAB_REPORT_VIEW]
AS
SELECT a.MasterId, a.InvNo, a.InvDate, a.LabNo, a.TestCode, a.Parameter, a.AliasNo, a.Result, a.Unit, a.NormalValue, a.ReportingGroupName, a.GroupSlNo, a.SerialNo, d.CollUser AS UserName, a.EntryDate, a.MachineName, a.ReportHeaderName, a.IsBold, a.IsPrint, c.ConsultantName, c.ConsultantDegree, c.CheckedByName, c.CheckedByDegree,c.LabInchargeName,c.LabInchargeDegree, b.PatientName,b.Age,b.Sex,b.MobileNo,b.DrCode,b.DrName,b.BedNo,b.PtStatus,(SELECT distinct Specimen FROM tb_Parameter_Definition WHERE TestCode=a.TestCode)AS Specimen,a.ReportFileName,a.ReportNo,d.CollTime,c.Comments,d.RDeliveryDate,b.PatientId AS RegNo,a.ManualSampleNo,c.Organism,c.ColonyCount,c.Incubation,c.SpecificTest
FROM tb_MachineDataDtls a LEFT JOIN  tb_InvMaster b ON a.MasterId=b.Id
INNER JOIN tb_MachineDataMaster c ON  a.MasterId=c.MasterId  AND a.ReportNo=c.ReportNo
INNER JOIN tb_LabSampleStatusInfo d ON a.MasterId=d.MasterId AND a.TestCode=d.TestCode
AND LTRIM(a.Result)<>''
GO
/****** Object:  Table [dbo].[tb_VaqGroup]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_VaqGroup](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[GroupId] [int] NOT NULL,
	[GroupName] [nvarchar](150) NULL,
	[Name] [nvarchar](500) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_VacutainerSetup]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_VacutainerSetup](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[GroupId] [int] NOT NULL,
	[GroupName] [nvarchar](250) NULL,
	[VaqGroupId] [int] NOT NULL,
	[VaqName] [nvarchar](150) NULL,
	[ItemId] [nvarchar](50) NOT NULL,
	[ItemDesc] [nvarchar](550) NOT NULL,
	[MasterCode] [nvarchar](50) NULL,
	[WordFileName] [nvarchar](500) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_Group]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_Group](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[Name] [nvarchar](100) NOT NULL,
	[HeaderName] [nvarchar](500) NOT NULL,
 CONSTRAINT [PK_tb_Group_1] PRIMARY KEY CLUSTERED 
(
	[Name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  View [dbo].[VW_Get_Vaq_GroupName]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[VW_Get_Vaq_GroupName]
AS
SELECT a.GroupId,a.VaqGroupId,a.ItemId,a.ItemDesc,b.Name As GroupName,c.Name As VaqGroupName,a.MasterCode ,a.wordFileName
FROm tb_VacutainerSetup a LEFT JOIN tb_Group b ON a.GroupId=b.Id
LEFT JOIN tb_VaqGroup c ON a.VaqGroupId=c.Id
GO
/****** Object:  Table [dbo].[tb_LabInvestigationChart]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_LabInvestigationChart](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[PCode] [nvarchar](50) NOT NULL,
	[ShortDesc] [nvarchar](250) NULL,
	[SubSubDeptName] [nvarchar](50) NULL,
 CONSTRAINT [PK_tb_LabInvestigationChart] PRIMARY KEY CLUSTERED 
(
	[PCode] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  View [dbo].[VW_Sample_Process_Tracking]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[VW_Sample_Process_Tracking]
AS
SELECT a.MasterId,b.InvNo,b.InvDate, a.SampleNo AS LabNo,b.PatientName,b.Age,b.Sex,c.Pcode,c.ShortDesc,a.CollStatus,a.CollTime,a.CollUser,a.SendStatus,a.SendTime,a.SendUser,a.ReceiveInLabStatus, a.ReceiveInLabTime, a.ReceiveInLabUser, a.ReportProcessStatus, a.ReportProcessTime, a.ReportProcessUser, a.ReportPrintStatus, a.ReportPrintTime, a.ReportPrintUser, a.RReceiveInDelCounterStatus, a.RReceiveInDelCounterTime, a.RReceiveInDelCounterUser, a.DeliverToPatientStatus, a.DeliverToPatientTime, a.DeliverToPatientUser,a.ReportNo,a.VaqGroup,a.FinalStatus,b.PtStatus,a.EntryDate,a.BranchId
FROM tb_LabSampleStatusInfo  a LEFT JOIN tb_InvMaster b ON a.MasterId=b.Id 
LEFT JOIN tb_LabInvestigationChart c ON a.TestCode=c.PCode
GO
/****** Object:  View [dbo].[VW_CollStatus_By_MasterId]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE VIEW [dbo].[VW_CollStatus_By_MasterId]
AS
SELECT a.SampleNo AS LabNo,
SUBSTRING((SELECT y.ShortDesc +',' FROM tb_LabSampleStatusInfo x left join tb_LabInvestigationChart y on  x.TestCode=y.PCode WHERE x.SampleNo=a.SampleNo FOR XML PATH ('')),1,LEN((SELECT y.ShortDesc +',' FROM tb_LabSampleStatusInfo x left join tb_LabInvestigationChart y on  x.TestCode=y.PCode WHERE x.SampleNo=a.SampleNo FOR XML PATH ('')))-1)   As FullDesc 
,a.CollStatus,a.CollTime,b.PatientName,b.Age,b.Sex  ,a.MasterId,b.BedNo,b.DrName
FROM tb_LabSampleStatusInfo  a LEFT JOIN tb_InvMaster b ON a.MasterId=b.Id 
GROUP BY a.SampleNo,a.CollStatus,a.CollTime,b.PatientName,b.Age,b.Sex ,a.MasterId,b.BedNo,b.DrName
GO
/****** Object:  View [dbo].[VW_LAB_TESTMAPPING]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE View [dbo].[VW_LAB_TESTMAPPING] as
SELECT     a.InvNo as RefNo, b.labNo as InvNo,b.LabNo, a.InvDate,a.PatientID,a.PtStatus AS  PatientStatus, a.InvNo as PatientName,a.PatientName as Name, a.Age, a.Sex, a.DrCode,a.DrName, b.Code AS PCode, b.ShortDesc
FROM         tb_InvMaster AS a, tb_InvDetails AS b
WHERE     a.Id = b.MasterId and b.Valid=1
GO
/****** Object:  Table [dbo].[tb_MachineDataDtls_MicroMaster]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_MachineDataDtls_MicroMaster](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[MasterId] [int] NOT NULL,
	[LabNo] [nvarchar](50) NOT NULL,
	[ReportNo] [nvarchar](50) NOT NULL,
	[Code] [nvarchar](50) NOT NULL,
	[Organism] [nvarchar](500) NOT NULL,
	[ColonyCount] [nvarchar](500) NOT NULL,
	[Incubation] [nvarchar](500) NOT NULL,
	[SpecficTest] [nvarchar](500) NOT NULL,
	[UserName] [nvarchar](50) NOT NULL,
	[EntryDate] [datetime] NOT NULL,
	[BiochemistName] [nvarchar](500) NOT NULL,
	[BiochemistDegree] [nvarchar](500) NOT NULL,
	[ConsultantName] [nvarchar](500) NOT NULL,
	[ConsultantDegree] [nvarchar](500) NOT NULL,
	[LabInchargeName] [nvarchar](500) NULL,
	[LabInchargeDegree] [nvarchar](500) NULL,
	[Specimen] [nvarchar](50) NOT NULL,
	[Comments] [nvarchar](500) NOT NULL,
	[TestName] [nvarchar](500) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_MachineDataDtls_MicroDetail]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_MachineDataDtls_MicroDetail](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[MasterId] [int] NOT NULL,
	[PCode] [nvarchar](50) NOT NULL,
	[DrugName] [nvarchar](100) NOT NULL,
	[ZoneSize] [nvarchar](50) NOT NULL,
	[Enterpretation] [nvarchar](50) NOT NULL,
	[Organism] [nvarchar](500) NULL,
	[ColonyCount] [nvarchar](500) NULL,
	[Incubation] [nvarchar](500) NULL,
	[SpecificTest] [nvarchar](500) NULL
) ON [PRIMARY]
GO
/****** Object:  View [dbo].[VW_LAB_REPORT_MICROBIOLOGY]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[VW_LAB_REPORT_MICROBIOLOGY]
AS
SELECT distinct a.MasterId, a.LabNo, a.ReportNo, a.Code,c.Age,c.InvDate,c.InvNo,c.PatientName,c.Sex,c.BedNo,c.DrName,c.PtStatus,a.Organism, a.ColonyCount, a.Incubation, a.SpecficTest, a.UserName, a.EntryDate, a.BiochemistName, a.BiochemistDegree, a.ConsultantName, a.ConsultantDegree, a.Specimen, a.Comments ,b.DrugName,b.Enterpretation,b.ZoneSize,a.LabInchargeName,a.LabInchargeDegree,b.Id As SlNo,a.TestName,d.CollUser,d.CollTime,c.MobileNo,d.RDeliveryDate,c.PatientId AS RegNo
FROM tb_MachineDataDtls_MicroMaster a
LEFT JOIN tb_MachineDataDtls_MicroDetail b ON a.MasterId=b.MasterId AND a.Code=b.PCode
LEFT JOIN tb_InvMaster c ON a.MasterId=c.Id 
LEFT JOIN tb_LabSampleStatusInfo d ON a.LabNo=d.SampleNo AND a.Code=d.TestCode
GO
/****** Object:  Table [dbo].[MachineDataDtls]    Script Date: 3/12/2026 7:00:40 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[MachineDataDtls](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[LABNO] [nvarchar](50) NULL,
	[Parameter] [nvarchar](500) NULL,
	[AliasName] [nvarchar](500) NULL,
	[Result] [nvarchar](500) NULL,
	[MachineName] [nvarchar](500) NULL,
	[ParameterType] [nvarchar](50) NULL,
	[INVNO] [nvarchar](50) NULL,
	[EntryTime] [nvarchar](50) NULL,
	[patientName] [nvarchar](500) NOT NULL,
	[Age] [nvarchar](500) NOT NULL,
	[Sex] [nvarchar](500) NOT NULL,
	[Unit] [nvarchar](500) NOT NULL,
	[NormalValue] [nvarchar](500) NOT NULL,
	[SerialNo] [nvarchar](500) NOT NULL,
	[GroupSl] [nvarchar](500) NOT NULL,
	[valid] [nvarchar](500) NOT NULL,
	[MultiLineResult] [nvarchar](500) NOT NULL,
	[PatientId] [nvarchar](500) NOT NULL,
	[BedNo] [nvarchar](500) NOT NULL,
	[InvDate] [nvarchar](150) NULL,
	[RefNo] [nvarchar](100) NOT NULL,
	[IsTransfer] [int] NOT NULL,
	[ReportingGroup] [nvarchar](500) NOT NULL,
	[EntryDate] [datetime] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  View [dbo].[VW_GroupReport_Dimension]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE VIEW [dbo].[VW_GroupReport_Dimension] AS
SELECT * FROM   
(
    SELECT a.LABNO+'-'+a.ParameterType AS SampleNo,b.InvNo, b.InvDate AS InvDate,b.PatientName PatientName,AliasName,Result,MachineName,ParameterType, a.EntryDate
    FROM Machinedatadtls a LEFT JOIN VW_Sample_Process_Tracking b ON a.LABNO=b.LabNo 

) t 
PIVOT(
    Max(Result) 
    FOR AliasName IN ([AHDL],[ALB],[ALDL],[ALP],[ALT],[AMY],[AST],[BUN],[CA],[CHOL],[CK],[CL],[CREA],[CRP],[DBI],[ECO2],[GGT],[GLUC],[HBA1C],[IBCT],[IRON],[K],[LDL],[LIPL],[MG],[NA],[PHOS],[RISK],[TBI],[TCO2],[TGL],[TP],[Troponin I],[URCA])
) AS pivot_table;
GO
/****** Object:  View [dbo].[VW_ReportProcessList]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE VIEW [dbo].[VW_ReportProcessList]
AS
SELECT a.MasterId,a.ReportNo,STUFF((SELECT ',' +  CONVERT(varchar, ShortDesc) FROM VW_Sample_Process_Tracking m WHERE m.ReportNo=a.ReportNO  AND m.MasterId=a.MasterId FOR XML PATH('')), 1, 1, '') TestName ,a.ReportPrintUser,a.ReportProcessStatus AS Rps 
FROM VW_Sample_Process_Tracking a 
GROUP BY a.ReportNo,a.ReportPrintUser,a.MasterId,a.ReportProcessStatus
GO
/****** Object:  Table [dbo].[tb_Invoice_Sample_Issue]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_Invoice_Sample_Issue](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[IssueNo] [nvarchar](50) NOT NULL,
	[IssueDate] [date] NOT NULL,
	[SampleNo] [nvarchar](50) NOT NULL,
	[HospId] [int] NOT NULL,
	[TestCode] [nvarchar](50) NOT NULL,
	[UserName] [nvarchar](50) NOT NULL,
	[EntryTime] [datetime] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_Hospital]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_Hospital](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[Name] [nvarchar](250) NOT NULL
) ON [PRIMARY]
GO
/****** Object:  View [dbo].[VW_Get_Sample_Issue_List]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE VIEW [dbo].[VW_Get_Sample_Issue_List] AS

SELECT  distinct a.IssueNo,a.IssueDate,a.EntryTime, a.SampleNo,b.Name AS HospName,a.TestCode,c.ShortDesc AS TestName,d.PatientName,d.Age,d.Sex,d.InvNo,d.InvDate,a.hospId,a.UserName
FROM tb_Invoice_Sample_Issue  a 
LEFT JOIN tb_Hospital b ON a.HospId=b.Id
LEFT JOIN tb_LabInvestigationChart c ON a.TestCode=c.Pcode
LEFT JOIN VW_Sample_Process_Tracking d ON a.SampleNo=d.LabNo
GO
/****** Object:  View [dbo].[VW_Get_All_SampleListByMasterId]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE VIEW [dbo].[VW_Get_All_SampleListByMasterId]
AS

SELECT a.LabNo As SampleNo,a.MasterId,
STUFF((SELECT ',' +  CONVERT(varchar, ShortDesc) FROM VW_Sample_Process_Tracking m WHERE m.LabNo=a.LabNo     FOR XML PATH('')), 1, 1, '') TestName,FinalStatus
FROM VW_Sample_Process_Tracking a 
Group by a.LabNo,a.FinalStatus,a.MasterId
GO
/****** Object:  View [dbo].[VW_Data_For_NurseStation]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[VW_Data_For_NurseStation] AS
SELECT a.MasterId,a.InvNo,a.InvDate,a.LabNo, STUFF((SELECT ',' +  CONVERT(varchar, ShortDesc) FROM VW_Sample_Process_Tracking m WHERE m.LabNo=a.LabNo      FOR XML PATH('')), 1, 1, '') TestName 
FROM VW_Sample_Process_Tracking a
GROUP BY a.LabNo,a.InvNo,a.InvDate,a.LabNo,a.MasterId
GO
/****** Object:  View [dbo].[V_ALL_TEST]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[V_ALL_TEST]
AS
SELECT a.TestCode,a.TestName AS Description,b.GroupName,b.VaqGroupName AS VaqName, a.Parameter,Alias AS TestName,a.Unit,a.NormalValue 
FROM tb_Parameter_Definition a LEFT JOIN VW_Get_Vaq_GroupName b 
ON a.TestCode=b.ItemId
GO
/****** Object:  View [dbo].[VW_LISORDER]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[VW_LISORDER]
AS
SELECT b.Code AS Pcode,c.Parameter,b.LabNo ,a.InvDate,c.MachineName 
FROM tb_InvMaster a
LEFT JOIN tb_InvDetails  b ON a.Id=b.MasterId
INNER JOIN ChannelDefination c ON b.CODE=c.PCode
WHERE a.InvDate>'2024-07-08' AND c.MachineName IN ('VITROSECI' ,'VITROS350')
GO
/****** Object:  View [dbo].[VW_ECI_ORDER]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE VIEW [dbo].[VW_ECI_ORDER]
AS
SELECT b.code AS TestCode,c.Parameter,b.LabNo AS BarcodeNo,a.InvDate,c.MachineName 
FROM tb_InvMaster a
LEFT JOIN tb_InvDetails  b ON a.Id =b.MasterId 
INNER JOIN ChannelDefination c ON b.CODE=c.PCode
WHERE a.InvDate>'2024-07-08' AND c.MachineName IN ('VITROSECI' ,'VITROS350')
GO
/****** Object:  Table [dbo].[A_HeaderView]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[A_HeaderView](
	[InvNo] [nvarchar](50) NOT NULL,
	[InvDate] [date] NOT NULL,
	[PtName] [nvarchar](2500) NOT NULL,
	[Age] [nvarchar](50) NOT NULL,
	[Sex] [nvarchar](50) NOT NULL,
	[DrName] [nvarchar](500) NOT NULL,
	[BedNo] [nvarchar](500) NULL,
	[Description] [nvarchar](1500) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[A_Imaging_Report]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[A_Imaging_Report](
	[InvNo] [nvarchar](50) NULL,
	[InvDate] [nvarchar](50) NULL,
	[Name] [nvarchar](500) NULL,
	[Sex] [nvarchar](50) NULL,
	[Age] [nvarchar](50) NULL,
	[DrName] [nvarchar](1500) NULL,
	[TestName] [nvarchar](500) NULL,
	[ReportNo] [nvarchar](50) NULL,
	[UserName] [nvarchar](50) NULL,
	[TestCode] [nvarchar](50) NULL,
	[Specimen] [nvarchar](50) NULL,
	[BedNo] [nvarchar](50) NULL,
	[DeliverDate] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[A_MachineDataDtls]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[A_MachineDataDtls](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[LABNO] [nvarchar](50) NULL,
	[Parameter] [nvarchar](500) NULL,
	[AliasName] [nvarchar](500) NULL,
	[Result] [nvarchar](500) NULL,
	[MachineName] [nvarchar](500) NULL,
	[ParameterType] [nvarchar](50) NULL,
	[INVNO] [nvarchar](50) NULL,
	[EntryTime] [nvarchar](50) NULL,
	[patientName] [nvarchar](500) NOT NULL,
	[Age] [nvarchar](500) NOT NULL,
	[Sex] [nvarchar](500) NOT NULL,
	[Unit] [nvarchar](500) NOT NULL,
	[NormalValue] [nvarchar](500) NOT NULL,
	[SerialNo] [nvarchar](500) NOT NULL,
	[GroupSl] [nvarchar](500) NOT NULL,
	[valid] [nvarchar](500) NOT NULL,
	[MultiLineResult] [nvarchar](500) NOT NULL,
	[PatientId] [nvarchar](500) NOT NULL,
	[BedNo] [nvarchar](500) NOT NULL,
	[InvDate] [datetime] NOT NULL,
	[RefNo] [nvarchar](100) NOT NULL,
	[IsTransfer] [int] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[A_TMP_BarcodePrint]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[A_TMP_BarcodePrint](
	[InvNo] [nvarchar](50) NOT NULL,
	[InvDate] [date] NOT NULL,
	[SampleNo] [nvarchar](50) NULL,
	[PtName] [nvarchar](150) NULL,
	[CollDateString] [nvarchar](150) NULL,
	[PtSex] [nvarchar](50) NULL,
	[TestName] [nvarchar](250) NULL,
	[CollTime] [nvarchar](50) NOT NULL,
	[BarcodeImage] [image] NOT NULL,
	[UserName] [nvarchar](50) NOT NULL,
	[DrName] [nvarchar](500) NULL,
	[BedNo] [nvarchar](50) NULL,
	[PtType] [nvarchar](50) NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[A_TMP_Lab_ReportView]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[A_TMP_Lab_ReportView](
	[InvNo] [nvarchar](50) NULL,
	[InvDate] [date] NULL,
	[LabNo] [nvarchar](500) NULL,
	[TestCode] [nvarchar](500) NULL,
	[Parameter] [nvarchar](500) NULL,
	[AliasNo] [nvarchar](500) NULL,
	[Result] [nvarchar](500) NULL,
	[Unit] [nvarchar](500) NULL,
	[NormalValue] [nvarchar](500) NULL,
	[ReportingGroupName] [nvarchar](500) NULL,
	[GroupSlNo] [int] NULL,
	[SerialNo] [int] NULL,
	[ReportHeaderName] [nvarchar](500) NULL,
	[IsBold] [int] NULL,
	[IsPrint] [int] NULL,
	[ConsultantName] [nvarchar](500) NULL,
	[ConsultantDegree] [nvarchar](1000) NULL,
	[CheckedByName] [nvarchar](500) NULL,
	[CheckedByDegree] [nvarchar](1000) NULL,
	[LabInchargeName] [nvarchar](500) NULL,
	[LabInchargeDegree] [nvarchar](500) NULL,
	[PatientName] [nvarchar](500) NULL,
	[Age] [nvarchar](50) NULL,
	[Sex] [nvarchar](50) NULL,
	[MobileNo] [nvarchar](50) NULL,
	[DrCode] [nvarchar](50) NULL,
	[DrName] [nvarchar](500) NULL,
	[BedNo] [nvarchar](50) NULL,
	[PtStatus] [nvarchar](50) NULL,
	[Specimen] [nvarchar](50) NULL,
	[CommentsInv] [nvarchar](500) NULL,
	[ReportFileName] [nvarchar](50) NULL,
	[TestName] [nvarchar](500) NULL,
	[ReportNo] [nvarchar](50) NULL,
	[UserName] [nvarchar](50) NULL,
	[EntryDate] [datetime] NOT NULL,
	[CollTime] [nvarchar](500) NULL,
	[PrintBy] [nvarchar](300) NULL,
	[RegNo] [nvarchar](100) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[A_TMP_Sample_Process_Tracking]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[A_TMP_Sample_Process_Tracking](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[InvNo] [nvarchar](50) NOT NULL,
	[InvDate] [date] NOT NULL,
	[LabNo] [nvarchar](50) NOT NULL,
	[PatientName] [nvarchar](150) NOT NULL,
	[Age] [nvarchar](50) NOT NULL,
	[Sex] [nvarchar](50) NOT NULL,
	[ShortDesc] [nvarchar](150) NOT NULL,
	[Type] [nvarchar](50) NOT NULL,
	[CollStatus] [nvarchar](50) NOT NULL,
	[CollTime] [nvarchar](50) NOT NULL,
	[UserName] [nvarchar](50) NOT NULL,
	[FinalStatus] [nvarchar](100) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[A_TMP_TEST]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[A_TMP_TEST](
	[InvNo] [nvarchar](50) NULL,
	[SampleNo] [nvarchar](50) NULL,
	[CollTime] [datetime] NULL,
	[CollUser] [nvarchar](50) NULL,
	[TestCode] [nvarchar](50) NULL,
	[ShortDesc] [nvarchar](1500) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[A_TMP_UrinePrint]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[A_TMP_UrinePrint](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[InvoiceNo] [nvarchar](50) NULL,
	[LabNo] [nvarchar](50) NULL,
	[TestDate] [date] NULL,
	[Name] [nvarchar](150) NULL,
	[BedNo] [nvarchar](50) NULL,
	[Age] [nvarchar](50) NULL,
	[Sex] [nvarchar](50) NULL,
	[HeaderName] [nvarchar](500) NULL,
	[Abbr] [nvarchar](50) NULL,
	[ItemName] [nvarchar](200) NULL,
	[Mark] [nvarchar](50) NULL,
	[Result] [nvarchar](50) NULL,
	[Reference] [nvarchar](50) NULL,
	[Unit] [nvarchar](50) NULL,
	[Type] [nvarchar](50) NULL,
	[SerialNo] [int] NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[A_TMP_UrinePrint_Lower]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[A_TMP_UrinePrint_Lower](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[InvoiceNo] [nvarchar](50) NULL,
	[LabNo] [nvarchar](50) NULL,
	[TestDate] [date] NULL,
	[Name] [nvarchar](150) NULL,
	[BedNo] [nvarchar](50) NULL,
	[Age] [nvarchar](50) NULL,
	[Sex] [nvarchar](50) NULL,
	[HeaderName] [nvarchar](500) NULL,
	[Abbr] [nvarchar](50) NULL,
	[ItemName] [nvarchar](200) NULL,
	[Mark] [nvarchar](50) NULL,
	[Result] [nvarchar](50) NULL,
	[Reference] [nvarchar](50) NULL,
	[Unit] [nvarchar](50) NULL,
	[Type] [nvarchar](50) NULL,
	[SerialNo] [int] NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[CalculateDiffCount]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CalculateDiffCount](
	[Parameter] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[CurveValue]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[CurveValue](
	[InvNo] [nvarchar](50) NULL,
	[LABNO] [nvarchar](50) NULL,
	[Parameter] [varchar](50) NULL,
	[CurveResult] [nvarchar](max) NULL,
	[SA1c] [nvarchar](50) NULL,
	[HbF] [nvarchar](50) NULL,
	[Remarks] [nvarchar](600) NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Default_Culture_Result_Info]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Default_Culture_Result_Info](
	[Result] [nvarchar](50) NULL,
	[CultureParameter] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[DefaultResult]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[DefaultResult](
	[DeptName] [nvarchar](50) NULL,
	[Pcode] [nvarchar](50) NULL,
	[Parameter] [nvarchar](50) NULL,
	[Result] [nvarchar](300) NULL,
	[SLNO] [numeric](18, 0) NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Del_Record_Of_Sample]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Del_Record_Of_Sample](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[InvNo] [nvarchar](50) NOT NULL,
	[InvDate] [nvarchar](50) NOT NULL,
	[SampleNo] [nvarchar](50) NOT NULL,
	[Description] [nvarchar](500) NOT NULL,
	[UserName] [nvarchar](50) NOT NULL,
	[EntryDate] [datetime] NOT NULL,
	[HostName] [nvarchar](150) NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ICUDischargeCertificateHistory]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ICUDischargeCertificateHistory](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[DischargeNo] [nvarchar](50) NULL,
	[DischargeDate] [date] NULL,
	[DischargeTime] [nvarchar](20) NULL,
	[PatientId] [nvarchar](20) NULL,
	[PatientName] [nvarchar](200) NULL,
	[Age] [nvarchar](50) NULL,
	[BedNo] [nvarchar](50) NULL,
	[Address] [nvarchar](250) NULL,
	[BloodGroup] [varchar](10) NULL,
	[Admission] [date] NULL,
	[Transfer] [nvarchar](250) NULL,
	[PresentingComplain] [ntext] NULL,
	[FinalDiagnosis] [ntext] NULL,
	[PresentIllness] [ntext] NULL,
	[RBS] [nvarchar](50) NULL,
	[ECG] [nvarchar](50) NULL,
	[S_Electrolyte] [nvarchar](250) NULL,
	[CBC] [nvarchar](250) NULL,
	[S_Creatinine] [nvarchar](50) NULL,
	[L_Troponin] [nvarchar](50) NULL,
	[R_Troponin] [nvarchar](50) NULL,
	[C_Consciousness] [nvarchar](50) NULL,
	[C_Heart] [nvarchar](50) NULL,
	[C_BP] [nvarchar](50) NULL,
	[C_Lungs] [nvarchar](50) NULL,
	[C_Pulse] [nvarchar](50) NULL,
	[C_SpO2] [nvarchar](50) NULL,
	[E_Consciousness] [nvarchar](50) NULL,
	[E_Temp] [nvarchar](50) NULL,
	[E_BP] [nvarchar](50) NULL,
	[E_SpO2] [nvarchar](50) NULL,
	[E_Pulse] [nvarchar](50) NULL,
	[E_Heart] [nvarchar](50) NULL,
	[E_Resp_Rate] [nvarchar](50) NULL,
	[E_Lungs] [nvarchar](50) NULL,
	[E_PlanterResponse] [nvarchar](50) NULL,
	[E_Pupil] [nvarchar](50) NULL,
	[HoldDRUG] [ntext] NULL,
	[Note] [ntext] NULL,
	[Valid] [int] NOT NULL,
	[UserName] [nvarchar](50) NOT NULL,
	[BranchId] [int] NOT NULL,
	[UnitId] [int] NOT NULL,
	[EntryDateTime] [datetime] NULL,
	[LogUser] [nvarchar](50) NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[Id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ICUDischargeConsultantDetails]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ICUDischargeConsultantDetails](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[DischargeNo] [nvarchar](50) NULL,
	[DischargeDate] [date] NULL,
	[PatientId] [nvarchar](20) NULL,
	[DrCode] [nvarchar](50) NULL,
	[DrName] [nvarchar](250) NULL,
	[BranchId] [int] NOT NULL,
	[Valid] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[Id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ICUPatientDischargeTreatment]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ICUPatientDischargeTreatment](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[DischargeNo] [nvarchar](50) NULL,
	[DischargeDate] [date] NULL,
	[PatientId] [nvarchar](20) NULL,
	[MedicineName] [nvarchar](250) NULL,
	[DoseInfo] [nvarchar](250) NULL,
	[Days] [nvarchar](250) NULL,
	[RowId] [int] NULL,
	[BranchId] [int] NOT NULL,
	[Valid] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[Id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[InvestigationChart]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[InvestigationChart](
	[DeptName] [nvarchar](50) NULL,
	[PCode] [nvarchar](20) NOT NULL,
	[ShortDesc] [nvarchar](150) NULL,
 CONSTRAINT [PK_InvestigationChart] PRIMARY KEY CLUSTERED 
(
	[PCode] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[InvMaster]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[InvMaster](
	[ID] [int] IDENTITY(1,1) NOT NULL,
	[INVDATE] [date] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[InvUserConfig]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[InvUserConfig](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[UserName] [nvarchar](100) NOT NULL,
	[DeptId] [int] NOT NULL,
	[DeptName] [nvarchar](100) NOT NULL,
	[Valid] [int] NOT NULL,
	[PermitedBy] [nvarchar](150) NOT NULL,
	[PostingDateTime] [datetime] NULL,
	[BranchId] [int] NOT NULL,
	[UnitId] [int] NOT NULL,
UNIQUE NONCLUSTERED 
(
	[UserName] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[LAB_sample_Investigation]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[LAB_sample_Investigation](
	[InvNo] [nvarchar](50) NOT NULL,
	[InvDate] [datetime] NULL,
	[LabNo] [nvarchar](50) NOT NULL,
	[PatientName] [nvarchar](50) NULL,
	[Age] [nvarchar](50) NULL,
	[Sex] [nvarchar](50) NULL,
	[ItemDescription] [nvarchar](1000) NULL,
	[BedNo] [nvarchar](50) NULL,
	[SCStatus] [nvarchar](10) NULL,
	[SCTime] [nvarchar](30) NULL,
	[SCUserName] [nvarchar](50) NULL,
	[SRLStatus] [nvarchar](10) NULL,
	[SRLTime] [nvarchar](30) NULL,
	[SRLUserName] [nvarchar](50) NULL,
	[SWPStatus] [nvarchar](10) NULL,
	[SWPTime] [nvarchar](30) NULL,
	[SRPStatus] [nvarchar](10) NULL,
	[SRPTime] [nvarchar](30) NULL,
	[SRPserName] [nvarchar](50) NULL,
	[RPStatus] [nvarchar](10) NULL,
	[RPTime] [nvarchar](30) NULL,
	[RPUserName] [nvarchar](50) NULL,
	[RDStatus] [nvarchar](10) NULL,
	[RDTime] [nvarchar](30) NULL,
	[RDUserName] [nvarchar](50) NULL,
	[DPStatus] [nvarchar](10) NULL,
	[DPTime] [nvarchar](30) NULL,
	[DPUserName] [nvarchar](50) NULL,
	[VaqGroup] [nvarchar](50) NULL,
	[VaqName] [nvarchar](50) NULL,
	[Rpt_Sample_No] [nvarchar](12) NULL,
	[Status] [nvarchar](20) NULL,
	[Step] [int] NULL,
	[PDate] [nvarchar](30) NULL,
	[SVerified] [nvarchar](20) NULL,
	[SVerifiedTime] [datetime] NULL,
	[PYEAR] [numeric](18, 0) NOT NULL,
	[EntryDate] [datetime] NULL,
	[PIDATE] [nvarchar](50) NULL,
	[EntryTime] [datetime] NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[MachineData]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[MachineData](
	[LabNo] [nvarchar](50) NULL,
	[TName] [nvarchar](50) NULL,
	[Result] [nvarchar](50) NULL,
	[IsTransfer] [int] NULL,
	[EntryTime] [datetime] NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[MachineDataDtls_LOG]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[MachineDataDtls_LOG](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[LABNO] [nvarchar](50) NULL,
	[Parameter] [nvarchar](500) NULL,
	[AliasName] [nvarchar](500) NULL,
	[Result] [nvarchar](500) NULL,
	[MachineName] [nvarchar](500) NULL,
	[ParameterType] [nvarchar](50) NULL,
	[INVNO] [nvarchar](50) NULL,
	[EntryTime] [nvarchar](50) NULL,
	[patientName] [nvarchar](500) NOT NULL,
	[Age] [nvarchar](500) NOT NULL,
	[Sex] [nvarchar](500) NOT NULL,
	[Unit] [nvarchar](500) NOT NULL,
	[NormalValue] [nvarchar](500) NOT NULL,
	[SerialNo] [nvarchar](500) NOT NULL,
	[GroupSl] [nvarchar](500) NOT NULL,
	[valid] [nvarchar](500) NOT NULL,
	[MultiLineResult] [nvarchar](500) NOT NULL,
	[PatientId] [nvarchar](500) NOT NULL,
	[BedNo] [nvarchar](500) NOT NULL,
	[InvDate] [datetime] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Microbiology_Normal_Range_Setup]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Microbiology_Normal_Range_Setup](
	[Pcode] [nchar](10) NULL,
	[ShortDesc] [nvarchar](50) NULL,
	[Parameter] [nvarchar](50) NULL,
	[FromValue] [numeric](18, 0) NULL,
	[ToValue] [numeric](18, 0) NULL,
	[Result] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[NICUDischargeCertificateHistory]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[NICUDischargeCertificateHistory](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[DischargeNo] [nvarchar](50) NULL,
	[DischargeType] [nvarchar](200) NULL,
	[DischargeDate] [date] NULL,
	[DischargeTime] [nvarchar](20) NULL,
	[PatientId] [nvarchar](50) NULL,
	[PatientName] [nvarchar](200) NULL,
	[GuardianName] [nvarchar](200) NULL,
	[Age] [nvarchar](50) NULL,
	[BedNo] [nvarchar](50) NULL,
	[Address] [nvarchar](250) NULL,
	[BloodGroup] [varchar](10) NULL,
	[Admission] [date] NULL,
	[Gender] [nvarchar](20) NULL,
	[Weight] [nvarchar](20) NULL,
	[PresentingComplain] [ntext] NULL,
	[FinalDiagnosis] [ntext] NULL,
	[HeartRate] [nvarchar](100) NULL,
	[RespiratoryRate] [nvarchar](100) NULL,
	[SpO2] [nvarchar](100) NULL,
	[Investigation] [ntext] NULL,
	[Valid] [int] NOT NULL,
	[UserName] [nvarchar](50) NOT NULL,
	[BranchId] [int] NOT NULL,
	[UnitId] [int] NOT NULL,
	[EntryDateTime] [datetime] NULL,
	[LogUser] [nvarchar](50) NOT NULL,
	[BirthHistory] [nvarchar](max) NULL,
	[SpecialMedication] [nvarchar](max) NULL,
PRIMARY KEY CLUSTERED 
(
	[Id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[NICUDischargeConsultantDetails]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[NICUDischargeConsultantDetails](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[DischargeNo] [nvarchar](50) NULL,
	[DischargeDate] [date] NULL,
	[PatientId] [nvarchar](20) NULL,
	[DrCode] [nvarchar](50) NULL,
	[DrName] [nvarchar](250) NULL,
	[BranchId] [int] NOT NULL,
	[Valid] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[Id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[NICUPatientDischargeTreatment]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[NICUPatientDischargeTreatment](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[DischargeNo] [nvarchar](50) NULL,
	[DischargeDate] [date] NULL,
	[PatientId] [nvarchar](20) NULL,
	[MedicineName] [nvarchar](250) NULL,
	[DoseInfo] [nvarchar](250) NULL,
	[Days] [nvarchar](250) NULL,
	[RowId] [int] NULL,
	[BranchId] [int] NOT NULL,
	[Valid] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[Id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[NICUTransferNote]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[NICUTransferNote](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[TransferNo] [nvarchar](50) NULL,
	[TransferDate] [date] NULL,
	[TransferTime] [nvarchar](20) NULL,
	[PatientId] [nvarchar](20) NULL,
	[PatientName] [nvarchar](200) NULL,
	[Age] [nvarchar](50) NULL,
	[BloodGroup] [varchar](10) NULL,
	[Weight] [varchar](10) NULL,
	[Admission] [date] NULL,
	[S_Electrolyte] [nvarchar](250) NULL,
	[CBC] [nvarchar](250) NULL,
	[CRP] [nvarchar](50) NULL,
	[CXR] [nvarchar](50) NULL,
	[S_Creatinine] [nvarchar](50) NULL,
	[S_Calcium] [nvarchar](50) NULL,
	[S_Bilirubin] [nvarchar](50) NULL,
	[Others] [nvarchar](50) NULL,
	[PresentingComplain] [ntext] NULL,
	[FinalDiagnosis] [ntext] NULL,
	[Note] [ntext] NULL,
	[Valid] [int] NOT NULL,
	[UserName] [nvarchar](50) NOT NULL,
	[BranchId] [int] NOT NULL,
	[UnitId] [int] NOT NULL,
	[EntryDateTime] [datetime] NULL,
	[LogUser] [nvarchar](50) NOT NULL,
	[DischargeType] [nvarchar](150) NULL,
PRIMARY KEY CLUSTERED 
(
	[Id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[NICUTransferNoteConsultants]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[NICUTransferNoteConsultants](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[TransferNo] [nvarchar](50) NULL,
	[TransferDate] [date] NULL,
	[PatientId] [nvarchar](20) NULL,
	[DrCode] [nvarchar](50) NULL,
	[DrName] [nvarchar](250) NULL,
	[BranchId] [int] NOT NULL,
	[Valid] [int] NOT NULL,
PRIMARY KEY CLUSTERED 
(
	[Id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[OPDTicketDueCollection]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[OPDTicketDueCollection](
	[Id] [int] NOT NULL,
	[OpdTicketId] [int] NOT NULL,
	[RefDate] [date] NOT NULL,
	[RefNo] [varchar](50) NOT NULL,
	[Charge] [money] NULL,
	[Less] [money] NULL,
	[Received] [money] NULL,
	[CashAmt] [money] NOT NULL,
	[CardAmt] [money] NOT NULL,
	[ChequeAmt] [money] NOT NULL,
	[CardNo] [varchar](50) NULL,
	[CardBank] [varchar](250) NULL,
	[ChequeNo] [varchar](50) NULL,
	[ChequeBank] [varchar](250) NULL,
	[ChequeDate] [nvarchar](50) NULL,
	[LessAmt] [money] NULL,
	[LessFrom] [varchar](150) NULL,
	[Remarks] [varchar](150) NULL,
	[BranchId] [int] NULL,
	[Valid] [int] NOT NULL,
	[UserName] [varchar](50) NULL,
	[EntryDateTime] [datetime] NULL,
	[LogDtls] [varchar](150) NULL,
PRIMARY KEY CLUSTERED 
(
	[Id] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[OPDTicketLedger]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[OPDTicketLedger](
	[OpdTicketId] [int] NOT NULL,
	[RefDate] [date] NOT NULL,
	[RefNo] [varchar](50) NOT NULL,
	[Charge] [money] NULL,
	[Less] [money] NULL,
	[Received] [money] NULL,
	[BranchId] [int] NULL,
	[Valid] [int] NOT NULL,
	[UserName] [varchar](50) NULL,
	[EntryTime] [datetime] NULL,
	[LessAdjust] [money] NOT NULL,
	[DueAdjust] [money] NOT NULL,
	[CashReturn] [money] NOT NULL,
	[InputFrom] [varchar](20) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Parameter_Microbiology]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Parameter_Microbiology](
	[Pcode] [nvarchar](50) NULL,
	[ShortDesc] [nvarchar](100) NULL,
	[L_Parameter] [nvarchar](50) NULL,
	[L_TestName] [nvarchar](50) NULL,
	[L_Result_A] [nchar](10) NULL,
	[L_Result_B] [nchar](10) NULL,
	[R_Parameter] [nvarchar](50) NULL,
	[R_TestName] [nvarchar](50) NULL,
	[R_Result_A] [nchar](10) NULL,
	[R_Result_B] [nchar](10) NULL,
	[L_SerialNo] [numeric](18, 0) NULL,
	[R_SerialNo] [numeric](18, 0) NULL,
	[CultureResult] [nvarchar](300) NULL,
	[OrganismRemarks] [nvarchar](300) NULL,
	[Specimen] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[parmCalculte]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[parmCalculte](
	[Pcode] [nvarchar](10) NULL,
	[Parameter] [nvarchar](20) NULL,
	[Fstprm] [nvarchar](20) NULL,
	[Sndprm] [nvarchar](20) NULL,
	[MultipleValue] [numeric](18, 0) NULL,
	[Type] [nvarchar](20) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[PortSetting]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[PortSetting](
	[BaudRate] [numeric](18, 0) NULL,
	[StopBit] [numeric](18, 0) NULL,
	[MachineName] [nvarchar](50) NULL,
	[Title] [nvarchar](50) NULL,
	[ShortName] [nvarchar](50) NULL,
	[EXE] [nvarchar](50) NULL,
	[PORTNO] [nvarchar](50) NULL,
	[HOST_NAME] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ProcedureRequest]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ProcedureRequest](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[RefDate] [date] NOT NULL,
	[RefNo] [varchar](30) NOT NULL,
	[PatientId] [varchar](30) NOT NULL,
	[RegNo] [varchar](30) NOT NULL,
	[BedNo] [varchar](150) NOT NULL,
	[Code] [varchar](20) NOT NULL,
	[Particulars] [varchar](250) NOT NULL,
	[DrCode] [varchar](20) NOT NULL,
	[DrName] [varchar](128) NOT NULL,
	[Charge] [money] NOT NULL,
	[NoU] [int] NOT NULL,
	[ServiceCharge] [money] NOT NULL,
	[TotalAmount] [money] NOT NULL,
	[DrAmount] [money] NOT NULL,
	[Status] [nvarchar](50) NOT NULL,
	[IsApproved] [int] NULL,
	[Valid] [tinyint] NOT NULL,
	[RowId] [int] NOT NULL,
	[UserName] [varchar](50) NOT NULL,
	[Remarks] [varchar](200) NOT NULL,
	[EntryDateTime] [datetime] NULL,
	[UserDtls] [nvarchar](200) NULL,
	[BranchId] [int] NOT NULL,
	[ChangedBedId] [int] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[ProcedureRequestApprove]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[ProcedureRequestApprove](
	[RequestNo] [varchar](20) NULL,
	[RequestDate] [date] NOT NULL,
	[RefNo] [varchar](30) NOT NULL,
	[RefDate] [date] NOT NULL,
	[PatientId] [varchar](30) NOT NULL,
	[RegNo] [varchar](30) NOT NULL,
	[BedNo] [varchar](150) NOT NULL,
	[Code] [varchar](20) NOT NULL,
	[Particulars] [varchar](250) NOT NULL,
	[DrCode] [varchar](20) NOT NULL,
	[DrName] [varchar](128) NOT NULL,
	[Charge] [money] NOT NULL,
	[NoU] [int] NOT NULL,
	[ServiceCharge] [money] NOT NULL,
	[TotalAmount] [money] NOT NULL,
	[DrAmount] [money] NOT NULL,
	[Status] [nvarchar](50) NOT NULL,
	[Valid] [tinyint] NOT NULL,
	[RowId] [int] NOT NULL,
	[UserName] [varchar](50) NOT NULL,
	[Remarks] [varchar](200) NOT NULL,
	[EntryDateTime] [datetime] NULL,
	[UserDtls] [nvarchar](200) NULL,
	[BranchId] [int] NOT NULL,
	[ChangedBedId] [int] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[RoundFigureConfig]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[RoundFigureConfig](
	[Pcode] [nvarchar](50) NULL,
	[Parameter] [nvarchar](50) NULL,
	[MachineName] [nvarchar](50) NULL,
	[SerialNo] [int] NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SerialNoMaintenance]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SerialNoMaintenance](
	[FixedAssetSerailNo] [int] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[SubGlucInfo]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[SubGlucInfo](
	[MasterPcode] [nvarchar](10) NULL,
	[MasterDec] [nvarchar](70) NULL,
	[PCODE] [nvarchar](10) NULL,
	[ShortDesc] [nvarchar](70) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_CurveResult]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_CurveResult](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[InvNo] [nvarchar](50) NOT NULL,
	[ParameterName] [nvarchar](50) NOT NULL,
	[Image] [varchar](max) NULL
) ON [PRIMARY] TEXTIMAGE_ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_Default_Comment_Setup]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_Default_Comment_Setup](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[Comments] [nvarchar](2500) NOT NULL,
	[IsShow] [int] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_DefaultLabDoctorSetting]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_DefaultLabDoctorSetting](
	[CheckedBy] [int] NOT NULL,
	[Consultant] [int] NOT NULL,
	[LabInCharge] [int] NOT NULL,
	[MachineName] [nvarchar](100) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_DefaultResultSetup]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_DefaultResultSetup](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[PCode] [nvarchar](50) NULL,
	[Name] [nvarchar](50) NOT NULL,
	[Result] [nvarchar](100) NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_DefaultResultSetupCulture]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_DefaultResultSetupCulture](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[Result] [nvarchar](200) NOT NULL,
	[Parameter] [nvarchar](50) NOT NULL,
	[IsShow] [int] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_DoctorSetup]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_DoctorSetup](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[Name] [nvarchar](250) NOT NULL,
	[Details] [nvarchar](500) NOT NULL,
	[Type] [nvarchar](50) NOT NULL,
	[IsShow] [int] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_Lab_Requisition_Print_Status]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_Lab_Requisition_Print_Status](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[InvNo] [nvarchar](50) NOT NULL,
	[IsPrint] [int] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_MachineSetup]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_MachineSetup](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[Name] [nvarchar](100) NOT NULL,
	[HeaderName] [nvarchar](500) NULL,
 CONSTRAINT [PK_tb_MachineSetup] PRIMARY KEY CLUSTERED 
(
	[Name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_MASTER_INFO]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_MASTER_INFO](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[ComName] [nvarchar](100) NULL,
	[Address] [nvarchar](150) NULL,
	[IsUsedBarcode] [int] NOT NULL,
	[ReportNo] [numeric](18, 0) NOT NULL,
	[IpdSampleNo] [numeric](18, 0) NOT NULL,
	[OpdSampleNo] [numeric](18, 0) NOT NULL,
	[INR] [float] NOT NULL,
	[DigitBlockInReport] [nvarchar](50) NULL,
	[DigitBlockInStricker] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_MasterCodeSetup]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_MasterCodeSetup](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[MasterPCode] [nvarchar](50) NOT NULL,
	[MasterDesc] [nvarchar](150) NOT NULL,
	[ChildCode] [nvarchar](50) NOT NULL,
	[ChildDesc] [nvarchar](150) NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_Other_GroupTest_ForDeliver]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_Other_GroupTest_ForDeliver](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[InvNo] [nvarchar](50) NOT NULL,
	[InvDate] [date] NOT NULL,
	[Pcode] [nvarchar](50) NOT NULL,
	[ShortDesc] [nvarchar](500) NULL,
	[CollUser] [nvarchar](150) NULL,
	[CollTime] [nvarchar](150) NULL,
	[IsReceive] [int] NOT NULL,
	[ReceiveTime] [nvarchar](150) NULL,
	[ReceiveUser] [nvarchar](150) NULL,
	[IsDeliver] [int] NOT NULL,
	[DeliverTime] [nvarchar](150) NULL,
	[DeliverUser] [nvarchar](150) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_OutTestMappingWithHospital]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_OutTestMappingWithHospital](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[HospitalId] [int] NOT NULL,
	[TestCode] [nvarchar](50) NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_Parameter_Definition_Microbiology]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_Parameter_Definition_Microbiology](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[ShortName] [nvarchar](50) NOT NULL,
	[Parameter] [nvarchar](100) NOT NULL,
	[IsShow] [int] NOT NULL,
 CONSTRAINT [PK_tb_Parameter_Definition_Microbiology] PRIMARY KEY CLUSTERED 
(
	[Parameter] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_Parameter_Definition_Microbiology_Default_Result]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_Parameter_Definition_Microbiology_Default_Result](
	[MasterId] [int] NOT NULL,
	[LowerVal] [float] NOT NULL,
	[UpperVal] [float] NOT NULL,
	[Result] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_ParameterValueInfo]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_ParameterValueInfo](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[Parameter] [nvarchar](50) NOT NULL,
	[LowerValue] [float] NOT NULL,
	[UpperValue] [float] NOT NULL,
	[Result] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_ReportingGroupSetup]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_ReportingGroupSetup](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[Name] [nvarchar](100) NOT NULL,
 CONSTRAINT [PK_tb_ReportingGroupSetup] PRIMARY KEY CLUSTERED 
(
	[Name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_SampleStatusInfo]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_SampleStatusInfo](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[Name] [nvarchar](50) NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_SpecimenSetup]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_SpecimenSetup](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[Name] [nvarchar](50) NOT NULL,
 CONSTRAINT [PK_tb_SpecimenSetup] PRIMARY KEY CLUSTERED 
(
	[Name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_UnitSetup]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_UnitSetup](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[Name] [nvarchar](50) NOT NULL,
 CONSTRAINT [PK_tb_UnitSetup] PRIMARY KEY CLUSTERED 
(
	[Name] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, IGNORE_DUP_KEY = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_UserAccess]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_UserAccess](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[UserName] [nvarchar](50) NOT NULL,
	[Password] [nvarchar](50) NOT NULL,
	[ParentName] [nvarchar](50) NOT NULL,
	[ChildName] [nvarchar](50) NOT NULL,
	[AuthorizedBy] [nvarchar](50) NOT NULL,
	[EntryDate] [datetime] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_UserAccess_Groupwise]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_UserAccess_Groupwise](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[UserName] [nvarchar](50) NOT NULL,
	[GroupName] [nvarchar](50) NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tb_VitrosOrder]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tb_VitrosOrder](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[LabNo] [varchar](50) NOT NULL,
	[Parameter] [nvarchar](500) NULL,
	[EntryDate] [datetime] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tbl_Pending_Sample_Configure_List]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tbl_Pending_Sample_Configure_List](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[InvNo] [nvarchar](50) NOT NULL,
	[InvDate] [date] NOT NULL,
	[MasterId] [int] NULL,
	[TestCode] [nvarchar](50) NOT NULL,
	[IsTransfer] [int] NOT NULL,
	[UserName] [nvarchar](50) NOT NULL,
	[EntryDate] [datetime] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[tmpGroupReport]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[tmpGroupReport](
	[InvNo] [nvarchar](50) NULL,
	[InvDate] [smalldatetime] NULL,
	[SIDNo] [nvarchar](50) NULL,
	[GLU] [nvarchar](50) NULL,
	[TBIL] [nvarchar](50) NULL,
	[AST] [nvarchar](50) NULL,
	[ALP] [nvarchar](50) NULL,
	[ALT] [nvarchar](50) NULL,
	[NA] [nvarchar](50) NULL,
	[K] [nvarchar](50) NULL,
	[Cl] [nvarchar](50) NULL,
	[ECO2] [nvarchar](50) NULL,
	[URCEA] [nvarchar](50) NULL,
	[CREA] [nvarchar](50) NULL,
	[TP] [nvarchar](50) NULL,
	[ALB] [nvarchar](50) NULL,
	[GLUB] [nvarchar](50) NULL,
	[AG] [nvarchar](50) NULL,
	[CA] [nvarchar](50) NULL,
	[PHOS] [nvarchar](50) NULL,
	[CHOL] [nvarchar](50) NULL,
	[AHDl] [nvarchar](50) NULL,
	[LDL] [nvarchar](50) NULL,
	[TGL] [nvarchar](50) NULL,
	[HA1C] [nvarchar](50) NULL,
	[GLUC] [nvarchar](50) NULL,
	[TBI] [nvarchar](50) NULL,
	[ALPI] [nvarchar](50) NULL,
	[IBCT] [nvarchar](50) NULL,
	[BUN] [nvarchar](50) NULL,
	[LIPL] [nvarchar](50) NULL,
	[ALDL] [nvarchar](50) NULL,
	[URCA] [nvarchar](50) NULL,
	[IRON] [nvarchar](50) NULL,
	[AMY] [nvarchar](50) NULL,
	[MG] [nvarchar](20) NULL,
	[ALTI] [nvarchar](20) NULL,
	[MLAB] [nvarchar](20) NULL,
	[MALB] [nvarchar](20) NULL,
	[DBI] [nvarchar](10) NULL,
	[Refno] [nvarchar](15) NULL,
	[HBA1C] [nvarchar](50) NULL,
	[LABNO] [nvarchar](20) NULL,
	[CRP] [nvarchar](50) NULL,
	[TCO2] [nvarchar](50) NULL,
	[Troponin I] [nvarchar](50) NULL,
	[CK] [nvarchar](50) NULL,
	[GGT] [nvarchar](50) NULL,
	[RA TEST] [nvarchar](50) NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[Update_Record_Of_Patient]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[Update_Record_Of_Patient](
	[Id] [int] IDENTITY(1,1) NOT NULL,
	[InvNo] [nvarchar](50) NOT NULL,
	[InvDate] [date] NOT NULL,
	[PatientName] [nvarchar](550) NOT NULL,
	[Age] [nvarchar](500) NOT NULL,
	[Sex] [nvarchar](500) NOT NULL,
	[MobileNo] [nvarchar](500) NOT NULL,
	[DrCode] [nvarchar](500) NOT NULL,
	[DrName] [nvarchar](500) NOT NULL,
	[UserName] [nvarchar](500) NOT NULL,
	[EntryDate] [datetime] NOT NULL
) ON [PRIMARY]
GO
/****** Object:  Table [dbo].[WorkingList]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE TABLE [dbo].[WorkingList](
	[BarcodeNo] [nvarchar](50) NULL,
	[OrderDate] [datetime] NULL,
	[PatientName] [nvarchar](150) NULL,
	[TName] [nvarchar](50) NULL,
	[MachineName] [nvarchar](50) NULL,
	[Result] [nvarchar](50) NULL,
	[SampleType] [nvarchar](50) NULL,
	[Status] [nvarchar](50) NULL,
	[OrderTime] [nvarchar](50) NULL
) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_MachineDataDtls]    Script Date: 3/12/2026 7:00:41 AM ******/
CREATE NONCLUSTERED INDEX [IX_MachineDataDtls] ON [dbo].[MachineDataDtls]
(
	[LABNO] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_tb_InvDetails]    Script Date: 3/12/2026 7:00:41 AM ******/
CREATE NONCLUSTERED INDEX [IX_tb_InvDetails] ON [dbo].[tb_InvDetails]
(
	[MasterId] DESC,
	[Code] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_tb_LabSampleStatusInfo]    Script Date: 3/12/2026 7:00:41 AM ******/
CREATE NONCLUSTERED INDEX [IX_tb_LabSampleStatusInfo] ON [dbo].[tb_LabSampleStatusInfo]
(
	[MasterId] ASC,
	[SampleNo] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_tb_MachineDataDtls]    Script Date: 3/12/2026 7:00:41 AM ******/
CREATE NONCLUSTERED INDEX [IX_tb_MachineDataDtls] ON [dbo].[tb_MachineDataDtls]
(
	[MasterId] DESC,
	[LabNo] DESC,
	[Parameter] ASC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
SET ANSI_PADDING ON
GO
/****** Object:  Index [IX_tb_MachineDataMaster]    Script Date: 3/12/2026 7:00:41 AM ******/
CREATE NONCLUSTERED INDEX [IX_tb_MachineDataMaster] ON [dbo].[tb_MachineDataMaster]
(
	[MasterId] DESC,
	[ReportNo] DESC
)WITH (PAD_INDEX = OFF, STATISTICS_NORECOMPUTE = OFF, SORT_IN_TEMPDB = OFF, DROP_EXISTING = OFF, ONLINE = OFF, ALLOW_ROW_LOCKS = ON, ALLOW_PAGE_LOCKS = ON, OPTIMIZE_FOR_SEQUENTIAL_KEY = OFF) ON [PRIMARY]
GO
ALTER TABLE [dbo].[A_MachineDataDtls] ADD  DEFAULT ((0)) FOR [IsTransfer]
GO
ALTER TABLE [dbo].[A_TMP_BarcodePrint] ADD  CONSTRAINT [DF_A_TMP_BarcodePrint_PtName]  DEFAULT ('') FOR [PtName]
GO
ALTER TABLE [dbo].[A_TMP_BarcodePrint] ADD  CONSTRAINT [DF_A_TMP_BarcodePrint_PtSex]  DEFAULT ('') FOR [PtSex]
GO
ALTER TABLE [dbo].[A_TMP_BarcodePrint] ADD  CONSTRAINT [DF_A_TMP_BarcodePrint_TestName]  DEFAULT ('') FOR [TestName]
GO
ALTER TABLE [dbo].[A_TMP_BarcodePrint] ADD  CONSTRAINT [DF_A_TMP_BarcodePrint_DrName]  DEFAULT (N'N/A') FOR [DrName]
GO
ALTER TABLE [dbo].[A_TMP_BarcodePrint] ADD  CONSTRAINT [DF_A_TMP_BarcodePrint_BedNo]  DEFAULT (N'N/A') FOR [BedNo]
GO
ALTER TABLE [dbo].[A_TMP_BarcodePrint] ADD  CONSTRAINT [DF_A_TMP_BarcodePrint_PtType]  DEFAULT (N'N/A') FOR [PtType]
GO
ALTER TABLE [dbo].[A_TMP_Lab_ReportView] ADD  CONSTRAINT [DF_A_TMP_Lab_ReportView_IsPrint]  DEFAULT ((0)) FOR [IsPrint]
GO
ALTER TABLE [dbo].[A_TMP_Lab_ReportView] ADD  CONSTRAINT [DF_A_TMP_Lab_ReportView_MobileNo]  DEFAULT ((0)) FOR [MobileNo]
GO
ALTER TABLE [dbo].[A_TMP_Lab_ReportView] ADD  CONSTRAINT [DF_A_TMP_Lab_ReportView_DrCode]  DEFAULT ('') FOR [DrCode]
GO
ALTER TABLE [dbo].[A_TMP_Lab_ReportView] ADD  CONSTRAINT [DF_A_TMP_Lab_ReportView_BedNo]  DEFAULT ('') FOR [BedNo]
GO
ALTER TABLE [dbo].[A_TMP_Lab_ReportView] ADD  CONSTRAINT [DF_A_TMP_Lab_ReportView_PtStatus]  DEFAULT ('') FOR [PtStatus]
GO
ALTER TABLE [dbo].[A_TMP_Lab_ReportView] ADD  CONSTRAINT [DF_A_TMP_Lab_ReportView_Specimen]  DEFAULT ('') FOR [Specimen]
GO
ALTER TABLE [dbo].[A_TMP_Lab_ReportView] ADD  CONSTRAINT [DF_A_TMP_Lab_ReportView_CommentsInv]  DEFAULT ('') FOR [CommentsInv]
GO
ALTER TABLE [dbo].[A_TMP_Lab_ReportView] ADD  CONSTRAINT [DF_A_TMP_Lab_ReportView_TestName]  DEFAULT ('') FOR [TestName]
GO
ALTER TABLE [dbo].[A_TMP_Lab_ReportView] ADD  CONSTRAINT [DF_A_TMP_Lab_ReportView_ReportNo]  DEFAULT ('') FOR [ReportNo]
GO
ALTER TABLE [dbo].[A_TMP_Lab_ReportView] ADD  CONSTRAINT [DF_A_TMP_Lab_ReportView_UserName]  DEFAULT ('') FOR [UserName]
GO
ALTER TABLE [dbo].[A_TMP_Lab_ReportView] ADD  CONSTRAINT [DF_A_TMP_Lab_ReportView_EntryDate]  DEFAULT (getdate()) FOR [EntryDate]
GO
ALTER TABLE [dbo].[A_TMP_UrinePrint] ADD  DEFAULT ((0)) FOR [SerialNo]
GO
ALTER TABLE [dbo].[A_TMP_UrinePrint_Lower] ADD  DEFAULT ((0)) FOR [SerialNo]
GO
ALTER TABLE [dbo].[Channeldefination] ADD  DEFAULT (getdate()) FOR [PostingDateTime]
GO
ALTER TABLE [dbo].[Del_Record_Of_Sample] ADD  CONSTRAINT [DF_Del_Record_Of_Sample_EntryDate]  DEFAULT (getdate()) FOR [EntryDate]
GO
ALTER TABLE [dbo].[Del_Record_Of_Sample] ADD  DEFAULT ('') FOR [HostName]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [DischargeNo]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [DischargeDate]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [DischargeTime]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [PatientId]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [PatientName]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [Age]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [BedNo]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [Address]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [BloodGroup]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [Admission]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [Transfer]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [PresentingComplain]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [FinalDiagnosis]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [PresentIllness]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [RBS]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [ECG]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [S_Electrolyte]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [CBC]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [S_Creatinine]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [L_Troponin]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [R_Troponin]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [C_Consciousness]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [C_Heart]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [C_BP]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [C_Lungs]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [C_Pulse]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [C_SpO2]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [E_Consciousness]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [E_Temp]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [E_BP]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [E_SpO2]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [E_Pulse]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [E_Heart]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [E_Resp_Rate]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [E_Lungs]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [E_PlanterResponse]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [E_Pupil]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [HoldDRUG]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [Note]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [UserName]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [BranchId]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ((1)) FOR [UnitId]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT (getdate()) FOR [EntryDateTime]
GO
ALTER TABLE [dbo].[ICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [LogUser]
GO
ALTER TABLE [dbo].[ICUDischargeConsultantDetails] ADD  DEFAULT ('') FOR [DischargeNo]
GO
ALTER TABLE [dbo].[ICUDischargeConsultantDetails] ADD  DEFAULT ('') FOR [DischargeDate]
GO
ALTER TABLE [dbo].[ICUDischargeConsultantDetails] ADD  DEFAULT ('') FOR [PatientId]
GO
ALTER TABLE [dbo].[ICUDischargeConsultantDetails] ADD  DEFAULT ('') FOR [DrCode]
GO
ALTER TABLE [dbo].[ICUDischargeConsultantDetails] ADD  DEFAULT ('') FOR [DrName]
GO
ALTER TABLE [dbo].[ICUDischargeConsultantDetails] ADD  DEFAULT ((1)) FOR [BranchId]
GO
ALTER TABLE [dbo].[ICUDischargeConsultantDetails] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[ICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [DischargeNo]
GO
ALTER TABLE [dbo].[ICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [DischargeDate]
GO
ALTER TABLE [dbo].[ICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [PatientId]
GO
ALTER TABLE [dbo].[ICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [MedicineName]
GO
ALTER TABLE [dbo].[ICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [DoseInfo]
GO
ALTER TABLE [dbo].[ICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [Days]
GO
ALTER TABLE [dbo].[ICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [RowId]
GO
ALTER TABLE [dbo].[ICUPatientDischargeTreatment] ADD  DEFAULT ((1)) FOR [BranchId]
GO
ALTER TABLE [dbo].[ICUPatientDischargeTreatment] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[InvUserConfig] ADD  DEFAULT ('') FOR [UserName]
GO
ALTER TABLE [dbo].[InvUserConfig] ADD  DEFAULT ((0)) FOR [DeptId]
GO
ALTER TABLE [dbo].[InvUserConfig] ADD  DEFAULT ('') FOR [DeptName]
GO
ALTER TABLE [dbo].[InvUserConfig] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[InvUserConfig] ADD  DEFAULT ('') FOR [PermitedBy]
GO
ALTER TABLE [dbo].[InvUserConfig] ADD  DEFAULT (getdate()) FOR [PostingDateTime]
GO
ALTER TABLE [dbo].[InvUserConfig] ADD  DEFAULT ((1)) FOR [BranchId]
GO
ALTER TABLE [dbo].[InvUserConfig] ADD  DEFAULT ((1)) FOR [UnitId]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_Table_1_SampCollStatus]  DEFAULT (N'Pending') FOR [SCStatus]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_Table_1_SampCollTime]  DEFAULT (N'Pending') FOR [SCTime]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_Table_1_SRcvInLabStatus]  DEFAULT (N'Pending') FOR [SRLStatus]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_SRLTime]  DEFAULT (N'Pending') FOR [SRLTime]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_SWPStatus]  DEFAULT (N'Pending') FOR [SWPStatus]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_SWPTime]  DEFAULT (N'Pending') FOR [SWPTime]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_SRPStatus]  DEFAULT (N'Pending') FOR [SRPStatus]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_SRPTime]  DEFAULT (N'Pending') FOR [SRPTime]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_RPStatus]  DEFAULT (N'Pending') FOR [RPStatus]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_RPTime]  DEFAULT (N'Pending') FOR [RPTime]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_RDStatus]  DEFAULT (N'Pending') FOR [RDStatus]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_RDTime]  DEFAULT (N'Pending') FOR [RDTime]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_DPStatus]  DEFAULT (N'Pending') FOR [DPStatus]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_DPTime]  DEFAULT (N'Pending') FOR [DPTime]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_Rpt_Sample_No]  DEFAULT ('') FOR [Rpt_Sample_No]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF__LAB_sampl__Statu__25518C17]  DEFAULT ('Collected') FOR [Status]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF__LAB_sample__Step__2645B050]  DEFAULT ((1)) FOR [Step]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF__LAB_sampl__PDate__2739D489]  DEFAULT (getdate()) FOR [PDate]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF__LAB_sampl__SVeri__540C7B00]  DEFAULT ('Pending') FOR [SVerified]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF__LAB_sampl__SVeri__55009F39]  DEFAULT (getdate()) FOR [SVerifiedTime]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_PYEAR]  DEFAULT (datepart(year,getdate())) FOR [PYEAR]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_EntryDate]  DEFAULT (getdate()) FOR [EntryDate]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF_LAB_sample_Investigation_PIDATE]  DEFAULT (getdate()) FOR [PIDATE]
GO
ALTER TABLE [dbo].[LAB_sample_Investigation] ADD  CONSTRAINT [DF__LAB_sampl__Entry__2BC97F7C]  DEFAULT (getdate()) FOR [EntryTime]
GO
ALTER TABLE [dbo].[MachineData] ADD  CONSTRAINT [DF_MachineData_IsTransfer]  DEFAULT ((0)) FOR [IsTransfer]
GO
ALTER TABLE [dbo].[MachineData] ADD  CONSTRAINT [DF_MachineData_EntryTime]  DEFAULT (getdate()) FOR [EntryTime]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDa__patie__1F63A897]  DEFAULT ('') FOR [patientName]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineData__Age__2057CCD0]  DEFAULT ('') FOR [Age]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineData__Sex__214BF109]  DEFAULT ('') FOR [Sex]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDat__Unit__22401542]  DEFAULT ('') FOR [Unit]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDa__Norma__2334397B]  DEFAULT ('') FOR [NormalValue]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDa__Seria__24285DB4]  DEFAULT ('0') FOR [SerialNo]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDa__Group__251C81ED]  DEFAULT ('0') FOR [GroupSl]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDa__valid__2610A626]  DEFAULT ('1') FOR [valid]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDa__Multi__2704CA5F]  DEFAULT ('') FOR [MultiLineResult]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDa__Patie__27F8EE98]  DEFAULT ('') FOR [PatientId]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDa__BedNo__28ED12D1]  DEFAULT ('') FOR [BedNo]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF_MachineDataDtls_InvDate]  DEFAULT (getdate()) FOR [InvDate]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDa__RefNo__4959E263]  DEFAULT ('') FOR [RefNo]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDa__IsTra__4B422AD5]  DEFAULT ((0)) FOR [IsTransfer]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  CONSTRAINT [DF__MachineDa__Repor__4D2A7347]  DEFAULT ('') FOR [ReportingGroup]
GO
ALTER TABLE [dbo].[MachineDataDtls] ADD  DEFAULT (getdate()) FOR [EntryDate]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT ('') FOR [patientName]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT ('') FOR [Age]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT ('') FOR [Sex]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT ('') FOR [Unit]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT ('') FOR [NormalValue]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT ('0') FOR [SerialNo]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT ('0') FOR [GroupSl]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT ('1') FOR [valid]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT ('') FOR [MultiLineResult]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT ('') FOR [PatientId]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT ('') FOR [BedNo]
GO
ALTER TABLE [dbo].[MachineDataDtls_LOG] ADD  DEFAULT (getdate()) FOR [InvDate]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [DischargeNo]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [DischargeType]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [DischargeDate]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [DischargeTime]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [PatientId]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [PatientName]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [GuardianName]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [Age]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [BedNo]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [Address]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [BloodGroup]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [Admission]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [Gender]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [Weight]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [PresentingComplain]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [FinalDiagnosis]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [HeartRate]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [RespiratoryRate]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [SpO2]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [Investigation]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [UserName]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [BranchId]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ((1)) FOR [UnitId]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT (getdate()) FOR [EntryDateTime]
GO
ALTER TABLE [dbo].[NICUDischargeCertificateHistory] ADD  DEFAULT ('') FOR [LogUser]
GO
ALTER TABLE [dbo].[NICUDischargeConsultantDetails] ADD  DEFAULT ('') FOR [DischargeNo]
GO
ALTER TABLE [dbo].[NICUDischargeConsultantDetails] ADD  DEFAULT ('') FOR [DischargeDate]
GO
ALTER TABLE [dbo].[NICUDischargeConsultantDetails] ADD  DEFAULT ('') FOR [PatientId]
GO
ALTER TABLE [dbo].[NICUDischargeConsultantDetails] ADD  DEFAULT ('') FOR [DrCode]
GO
ALTER TABLE [dbo].[NICUDischargeConsultantDetails] ADD  DEFAULT ('') FOR [DrName]
GO
ALTER TABLE [dbo].[NICUDischargeConsultantDetails] ADD  DEFAULT ((1)) FOR [BranchId]
GO
ALTER TABLE [dbo].[NICUDischargeConsultantDetails] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[NICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [DischargeNo]
GO
ALTER TABLE [dbo].[NICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [DischargeDate]
GO
ALTER TABLE [dbo].[NICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [PatientId]
GO
ALTER TABLE [dbo].[NICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [MedicineName]
GO
ALTER TABLE [dbo].[NICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [DoseInfo]
GO
ALTER TABLE [dbo].[NICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [Days]
GO
ALTER TABLE [dbo].[NICUPatientDischargeTreatment] ADD  DEFAULT ('') FOR [RowId]
GO
ALTER TABLE [dbo].[NICUPatientDischargeTreatment] ADD  DEFAULT ((1)) FOR [BranchId]
GO
ALTER TABLE [dbo].[NICUPatientDischargeTreatment] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [TransferNo]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [TransferDate]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [TransferTime]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [PatientId]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [PatientName]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [Age]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [BloodGroup]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [Weight]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [Admission]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [S_Electrolyte]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [CBC]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [CRP]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [CXR]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [S_Creatinine]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [S_Calcium]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [S_Bilirubin]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [Others]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [PresentingComplain]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [FinalDiagnosis]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [Note]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [UserName]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [BranchId]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ((1)) FOR [UnitId]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT (getdate()) FOR [EntryDateTime]
GO
ALTER TABLE [dbo].[NICUTransferNote] ADD  DEFAULT ('') FOR [LogUser]
GO
ALTER TABLE [dbo].[NICUTransferNoteConsultants] ADD  DEFAULT ('') FOR [TransferNo]
GO
ALTER TABLE [dbo].[NICUTransferNoteConsultants] ADD  DEFAULT ('') FOR [TransferDate]
GO
ALTER TABLE [dbo].[NICUTransferNoteConsultants] ADD  DEFAULT ('') FOR [PatientId]
GO
ALTER TABLE [dbo].[NICUTransferNoteConsultants] ADD  DEFAULT ('') FOR [DrCode]
GO
ALTER TABLE [dbo].[NICUTransferNoteConsultants] ADD  DEFAULT ('') FOR [DrName]
GO
ALTER TABLE [dbo].[NICUTransferNoteConsultants] ADD  DEFAULT ((1)) FOR [BranchId]
GO
ALTER TABLE [dbo].[NICUTransferNoteConsultants] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[OPDTicketDueCollection] ADD  DEFAULT (getdate()) FOR [RefDate]
GO
ALTER TABLE [dbo].[OPDTicketDueCollection] ADD  DEFAULT ((0)) FOR [CashAmt]
GO
ALTER TABLE [dbo].[OPDTicketDueCollection] ADD  DEFAULT ((0)) FOR [CardAmt]
GO
ALTER TABLE [dbo].[OPDTicketDueCollection] ADD  DEFAULT ((0)) FOR [ChequeAmt]
GO
ALTER TABLE [dbo].[OPDTicketDueCollection] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[OPDTicketDueCollection] ADD  DEFAULT (getdate()) FOR [EntryDateTime]
GO
ALTER TABLE [dbo].[OPDTicketLedger] ADD  DEFAULT (getdate()) FOR [RefDate]
GO
ALTER TABLE [dbo].[OPDTicketLedger] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[OPDTicketLedger] ADD  DEFAULT (getdate()) FOR [EntryTime]
GO
ALTER TABLE [dbo].[OPDTicketLedger] ADD  DEFAULT ((0)) FOR [LessAdjust]
GO
ALTER TABLE [dbo].[OPDTicketLedger] ADD  DEFAULT ((0)) FOR [DueAdjust]
GO
ALTER TABLE [dbo].[OPDTicketLedger] ADD  DEFAULT ((0)) FOR [CashReturn]
GO
ALTER TABLE [dbo].[Parameter_Microbiology] ADD  CONSTRAINT [DF_Parameter_Microbiology_CultureResult]  DEFAULT ('') FOR [CultureResult]
GO
ALTER TABLE [dbo].[Parameter_Microbiology] ADD  CONSTRAINT [DF_Parameter_Microbiology_OrganismRemarks]  DEFAULT ('') FOR [OrganismRemarks]
GO
ALTER TABLE [dbo].[Parameter_Microbiology] ADD  DEFAULT ('') FOR [Specimen]
GO
ALTER TABLE [dbo].[ProcedureRequest] ADD  DEFAULT (getdate()) FOR [RefDate]
GO
ALTER TABLE [dbo].[ProcedureRequest] ADD  DEFAULT ((0)) FOR [Charge]
GO
ALTER TABLE [dbo].[ProcedureRequest] ADD  DEFAULT ((1)) FOR [NoU]
GO
ALTER TABLE [dbo].[ProcedureRequest] ADD  DEFAULT ((0)) FOR [ServiceCharge]
GO
ALTER TABLE [dbo].[ProcedureRequest] ADD  DEFAULT ((0)) FOR [TotalAmount]
GO
ALTER TABLE [dbo].[ProcedureRequest] ADD  DEFAULT ((0)) FOR [DrAmount]
GO
ALTER TABLE [dbo].[ProcedureRequest] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[ProcedureRequest] ADD  DEFAULT ((1)) FOR [RowId]
GO
ALTER TABLE [dbo].[ProcedureRequest] ADD  DEFAULT (getdate()) FOR [EntryDateTime]
GO
ALTER TABLE [dbo].[ProcedureRequest] ADD  DEFAULT ((1)) FOR [BranchId]
GO
ALTER TABLE [dbo].[ProcedureRequest] ADD  DEFAULT ((0)) FOR [ChangedBedId]
GO
ALTER TABLE [dbo].[ProcedureRequestApprove] ADD  DEFAULT (getdate()) FOR [RefDate]
GO
ALTER TABLE [dbo].[ProcedureRequestApprove] ADD  DEFAULT ((0)) FOR [Charge]
GO
ALTER TABLE [dbo].[ProcedureRequestApprove] ADD  DEFAULT ((1)) FOR [NoU]
GO
ALTER TABLE [dbo].[ProcedureRequestApprove] ADD  DEFAULT ((0)) FOR [ServiceCharge]
GO
ALTER TABLE [dbo].[ProcedureRequestApprove] ADD  DEFAULT ((0)) FOR [TotalAmount]
GO
ALTER TABLE [dbo].[ProcedureRequestApprove] ADD  DEFAULT ((0)) FOR [DrAmount]
GO
ALTER TABLE [dbo].[ProcedureRequestApprove] ADD  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[ProcedureRequestApprove] ADD  DEFAULT ((1)) FOR [RowId]
GO
ALTER TABLE [dbo].[ProcedureRequestApprove] ADD  DEFAULT (getdate()) FOR [EntryDateTime]
GO
ALTER TABLE [dbo].[ProcedureRequestApprove] ADD  DEFAULT ((1)) FOR [BranchId]
GO
ALTER TABLE [dbo].[ProcedureRequestApprove] ADD  DEFAULT ((0)) FOR [ChangedBedId]
GO
ALTER TABLE [dbo].[SerialNoMaintenance] ADD  DEFAULT ((0)) FOR [FixedAssetSerailNo]
GO
ALTER TABLE [dbo].[tb_Default_Comment_Setup] ADD  CONSTRAINT [DF_tb_Default_Comment_Setup_IsShow]  DEFAULT ((1)) FOR [IsShow]
GO
ALTER TABLE [dbo].[tb_DefaultLabDoctorSetting] ADD  CONSTRAINT [DF_tb_DefaultLabDoctorSetting_LabInCharge]  DEFAULT ((0)) FOR [LabInCharge]
GO
ALTER TABLE [dbo].[tb_DefaultResultSetupCulture] ADD  CONSTRAINT [DF_tbl_DefaultResultSetupCulture_IsShow]  DEFAULT ((1)) FOR [IsShow]
GO
ALTER TABLE [dbo].[tb_DoctorSetup] ADD  CONSTRAINT [DF_tb_DoctorSetup_Type]  DEFAULT (N'N/A') FOR [Type]
GO
ALTER TABLE [dbo].[tb_DoctorSetup] ADD  CONSTRAINT [DF__tb_Doctor__IsSho__6E01572D]  DEFAULT ((1)) FOR [IsShow]
GO
ALTER TABLE [dbo].[tb_Group] ADD  CONSTRAINT [DF_tb_Group_HeaderName]  DEFAULT (N'N/A') FOR [HeaderName]
GO
ALTER TABLE [dbo].[tb_InvDetails] ADD  CONSTRAINT [DF_tb_InvDetails_VaqName]  DEFAULT (N'N/A') FOR [VaqName]
GO
ALTER TABLE [dbo].[tb_InvDetails] ADD  CONSTRAINT [DF_tb_InvDetails_LabNo]  DEFAULT ('') FOR [LabNo]
GO
ALTER TABLE [dbo].[tb_InvDetails] ADD  CONSTRAINT [DF_tb_InvDetails_PtYear]  DEFAULT (datepart(year,getdate())) FOR [PtYear]
GO
ALTER TABLE [dbo].[tb_InvDetails] ADD  CONSTRAINT [DF_tb_InvDetails_PrintNo]  DEFAULT ((0)) FOR [PrintNo]
GO
ALTER TABLE [dbo].[tb_InvDetails] ADD  CONSTRAINT [DF__tb_InvDet__IsSav__245D67DE]  DEFAULT ((0)) FOR [IsSaved]
GO
ALTER TABLE [dbo].[tb_InvDetails] ADD  CONSTRAINT [DF__tb_InvDet__Valid__3864608B]  DEFAULT ((1)) FOR [Valid]
GO
ALTER TABLE [dbo].[tb_InvMaster] ADD  CONSTRAINT [DF_tb_InvMaster_PtYear]  DEFAULT (datepart(year,getdate())) FOR [PtYear]
GO
ALTER TABLE [dbo].[tb_InvMaster] ADD  CONSTRAINT [DF_tb_InvMaster_EntryTime]  DEFAULT (getdate()) FOR [EntryTime]
GO
ALTER TABLE [dbo].[tb_InvMaster] ADD  CONSTRAINT [DF_tb_InvMaster_InvTime]  DEFAULT ('') FOR [InvTime]
GO
ALTER TABLE [dbo].[tb_InvMaster] ADD  CONSTRAINT [DF_tb_InvMaster_BranchId]  DEFAULT ((0)) FOR [BranchId]
GO
ALTER TABLE [dbo].[tb_Invoice_Sample_Issue] ADD  CONSTRAINT [DF_tb_Invoice_Sample_Issue_IssueDate]  DEFAULT (getdate()) FOR [IssueDate]
GO
ALTER TABLE [dbo].[tb_Invoice_Sample_Issue] ADD  CONSTRAINT [DF_tb_Invoice_Sample_Issue_EntryTime]  DEFAULT (getdate()) FOR [EntryTime]
GO
ALTER TABLE [dbo].[tb_Lab_Requisition_Print_Status] ADD  CONSTRAINT [DF_tb_Lab_Requisition_Print_Status_IsPrint]  DEFAULT ((1)) FOR [IsPrint]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_ReportNo]  DEFAULT (N'N/A') FOR [ReportNo]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_CollStatus]  DEFAULT (N'Pending') FOR [CollStatus]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_CollUser]  DEFAULT (N'Pending') FOR [CollUser]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_SendStatus]  DEFAULT (N'Pending') FOR [SendStatus]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_SendUser]  DEFAULT (N'Pending') FOR [SendUser]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_ReceiveInLabStatus]  DEFAULT (N'Pending') FOR [ReceiveInLabStatus]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_ReceiveInLabUser]  DEFAULT (N'Pending') FOR [ReceiveInLabUser]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_ReportPrintStatus]  DEFAULT (N'Pending') FOR [ReportPrintStatus]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_ReportPrintUser]  DEFAULT (N'Pending') FOR [ReportPrintUser]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_ReportProcessStatus]  DEFAULT (N'Pending') FOR [ReportProcessStatus]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_ReportProcessUser]  DEFAULT (N'Pending') FOR [ReportProcessUser]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_ReportDeliveryToCounterStatus]  DEFAULT (N'Pending') FOR [RReceiveInDelCounterStatus]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_ReportDeliveryToCounterUser]  DEFAULT (N'Pending') FOR [RReceiveInDelCounterUser]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_DeliverToPatientStatus]  DEFAULT (N'Pending') FOR [DeliverToPatientStatus]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_DeliverToPatientUser]  DEFAULT (N'Pending') FOR [DeliverToPatientUser]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  CONSTRAINT [DF_tb_LabSampleStatusInfo_EntryDate]  DEFAULT (getdate()) FOR [EntryDate]
GO
ALTER TABLE [dbo].[tb_LabSampleStatusInfo] ADD  DEFAULT ((1)) FOR [BranchId]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls] ADD  CONSTRAINT [DF_tb_MachineDataDtls_MasterId]  DEFAULT ((0)) FOR [MasterId]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls] ADD  CONSTRAINT [DF_tb_MachineDataDtls_InvDate]  DEFAULT (getdate()) FOR [InvDate]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls] ADD  CONSTRAINT [DF_tb_MachineDataDtls_GroupSlNo]  DEFAULT ((0)) FOR [GroupSlNo]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls] ADD  CONSTRAINT [DF_tb_MachineDataDtls_SerialNo]  DEFAULT ((0)) FOR [SerialNo]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls] ADD  CONSTRAINT [DF_tb_MachineDataDtls_EntryDate]  DEFAULT (getdate()) FOR [EntryDate]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls] ADD  CONSTRAINT [DF_tb_MachineDataDtls_IsBold]  DEFAULT ((0)) FOR [IsBold]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls] ADD  CONSTRAINT [DF_tb_MachineDataDtls_IsPrint]  DEFAULT ((0)) FOR [IsPrint]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls] ADD  CONSTRAINT [DF__tb_Machin__Manua__71D1E811]  DEFAULT ('') FOR [ManualSampleNo]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls_MicroMaster] ADD  CONSTRAINT [DF_tb_MachineDataDtls_MicroMaster_ReportNo]  DEFAULT (N'N/A') FOR [ReportNo]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls_MicroMaster] ADD  CONSTRAINT [DF_tb_MachineDataDtls_MicroMaster_Organism]  DEFAULT ('') FOR [Organism]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls_MicroMaster] ADD  CONSTRAINT [DF_tb_MachineDataDtls_MicroMaster_ColonyCount]  DEFAULT ('') FOR [ColonyCount]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls_MicroMaster] ADD  CONSTRAINT [DF_tb_MachineDataDtls_MicroMaster_Incubation]  DEFAULT ('') FOR [Incubation]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls_MicroMaster] ADD  CONSTRAINT [DF_tb_MachineDataDtls_MicroMaster_SpecficTest]  DEFAULT ('') FOR [SpecficTest]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls_MicroMaster] ADD  CONSTRAINT [DF_tb_MachineDataDtls_MicroMaster_EntryDate]  DEFAULT (getdate()) FOR [EntryDate]
GO
ALTER TABLE [dbo].[tb_MachineDataDtls_MicroMaster] ADD  CONSTRAINT [DF_tb_MachineDataDtls_MicroMaster_Comments]  DEFAULT ('') FOR [Comments]
GO
ALTER TABLE [dbo].[tb_MachineDataMaster] ADD  DEFAULT ('') FOR [Organism]
GO
ALTER TABLE [dbo].[tb_MachineDataMaster] ADD  DEFAULT ('') FOR [ColonyCount]
GO
ALTER TABLE [dbo].[tb_MachineDataMaster] ADD  DEFAULT ('') FOR [Incubation]
GO
ALTER TABLE [dbo].[tb_MachineDataMaster] ADD  DEFAULT ('') FOR [SpecificTest]
GO
ALTER TABLE [dbo].[tb_MachineDataMaster] ADD  DEFAULT ('') FOR [PCode]
GO
ALTER TABLE [dbo].[tb_MASTER_INFO] ADD  CONSTRAINT [DF_tb_MASTER_INFO_ReportNo]  DEFAULT ((0)) FOR [ReportNo]
GO
ALTER TABLE [dbo].[tb_MASTER_INFO] ADD  CONSTRAINT [DF_tb_MASTER_INFO_IpdSampleNo]  DEFAULT ((0)) FOR [IpdSampleNo]
GO
ALTER TABLE [dbo].[tb_MASTER_INFO] ADD  CONSTRAINT [DF_tb_MASTER_INFO_OpdSampleNo]  DEFAULT ((0)) FOR [OpdSampleNo]
GO
ALTER TABLE [dbo].[tb_MASTER_INFO] ADD  CONSTRAINT [DF_tb_MASTER_INFO_INR]  DEFAULT ((0)) FOR [INR]
GO
ALTER TABLE [dbo].[tb_Other_GroupTest_ForDeliver] ADD  CONSTRAINT [DF_tb_Other_GroupTest_ForDeliver_IsReceive]  DEFAULT ((0)) FOR [IsReceive]
GO
ALTER TABLE [dbo].[tb_Other_GroupTest_ForDeliver] ADD  CONSTRAINT [DF_tb_Other_GroupTest_ForDeliver_IsDelivere]  DEFAULT ((0)) FOR [IsDeliver]
GO
ALTER TABLE [dbo].[tb_Parameter_Definition] ADD  CONSTRAINT [DF_tb_Parameter_Definition_MachineName]  DEFAULT (N'MN') FOR [MachineName]
GO
ALTER TABLE [dbo].[tb_Parameter_Definition_Microbiology] ADD  CONSTRAINT [DF_tb_Parameter_Definition_Microbiology_ShortName]  DEFAULT ('') FOR [ShortName]
GO
ALTER TABLE [dbo].[tb_Parameter_Definition_Microbiology] ADD  CONSTRAINT [DF_tb_Parameter_Definition_Microbiology_IsShow]  DEFAULT ((1)) FOR [IsShow]
GO
ALTER TABLE [dbo].[tb_UserAccess] ADD  CONSTRAINT [DF_tb_UserAccess_EntryDate]  DEFAULT (getdate()) FOR [EntryDate]
GO
ALTER TABLE [dbo].[tb_VitrosOrder] ADD  CONSTRAINT [DF_tb_VitrosOrder_EntryDate]  DEFAULT (getdate()) FOR [EntryDate]
GO
ALTER TABLE [dbo].[tbl_Pending_Sample_Configure_List] ADD  CONSTRAINT [DF_tbl_Pending_Sample_Configure_List_IsTransfer]  DEFAULT ((0)) FOR [IsTransfer]
GO
ALTER TABLE [dbo].[tbl_Pending_Sample_Configure_List] ADD  CONSTRAINT [DF_tbl_Pending_Sample_Configure_List_EntryDate]  DEFAULT (getdate()) FOR [EntryDate]
GO
ALTER TABLE [dbo].[tmpGroupReport] ADD  CONSTRAINT [DF_tmpGroupReport_ALT]  DEFAULT ('') FOR [ALT]
GO
ALTER TABLE [dbo].[tmpGroupReport] ADD  DEFAULT ('') FOR [MG]
GO
ALTER TABLE [dbo].[tmpGroupReport] ADD  DEFAULT ('') FOR [ALTI]
GO
ALTER TABLE [dbo].[tmpGroupReport] ADD  DEFAULT ('') FOR [MLAB]
GO
ALTER TABLE [dbo].[tmpGroupReport] ADD  DEFAULT ('') FOR [MALB]
GO
ALTER TABLE [dbo].[tmpGroupReport] ADD  DEFAULT ('') FOR [DBI]
GO
ALTER TABLE [dbo].[tmpGroupReport] ADD  DEFAULT ('') FOR [Refno]
GO
ALTER TABLE [dbo].[Update_Record_Of_Patient] ADD  CONSTRAINT [DF_Update_Record_Of_Patient_EntryDate]  DEFAULT (getdate()) FOR [EntryDate]
GO
ALTER TABLE [dbo].[WorkingList] ADD  CONSTRAINT [DF_WorkingList_PatientName]  DEFAULT ('') FOR [PatientName]
GO
/****** Object:  StoredProcedure [dbo].[DEL_TABLE]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROC [dbo].[DEL_TABLE] AS
BEGIN
Truncate table tb_InvMaster 
Truncate table tb_InvDetails
Truncate table tb_MachineDataDtls
Truncate table tb_LabSampleStatusInfo
Truncate table tbl_Pending_Sample_Configure_List
Truncate table machineDataDtls
Truncate table machineDataDtls_LOG
Truncate table tb_MachineDataDtls_MicroDetail
Truncate table tb_MachineDataDtls_MicroMaster
Truncate table tb_MachineDataMaster

Truncate table WorkingList
Truncate table CurveValue
TRUNCATE TABLE Update_Record_Of_Patient
Truncate Table tb_Other_GroupTest_ForDeliver
END

GO
/****** Object:  StoredProcedure [dbo].[SP_Pending_ReportDeliveredToPatientInDeliveryCounter ]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO
CREATE PROC [dbo].[SP_Pending_ReportDeliveredToPatientInDeliveryCounter ] (@date date)AS
BEGIN
SELECT a.InvNo,a.InvDate,a.LabNo, a.ReportNo,STUFF((SELECT ',' +  CONVERT(varchar, ShortDesc) FROM VW_Sample_Process_Tracking m WHERE m.ReportNo=a.ReportNO      FOR XML PATH('')), 1, 1, '') TestName ,RReceiveInDelCounterTime,RReceiveInDelCounterUser
FROM VW_Sample_Process_Tracking a
WHERE a.RReceiveInDelCounterStatus='Received' AND a.DeliverToPatientStatus='Pending' AND Convert(date,a.RReceiveInDelCounterTime,108)=@date
GROUP BY a.ReportNo,a.InvNo,a.InvDate,a.LabNo,RReceiveInDelCounterTime,RReceiveInDelCounterUser
Order BY RReceiveInDelCounterTime asc
END
GO
/****** Object:  StoredProcedure [dbo].[SP_Pending_ReportProceList]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROC [dbo].[SP_Pending_ReportProceList] (@date date)AS
BEGIN
SELECT a.InvNo,a.InvDate, a.ReportNo,
STUFF((SELECT ',' +  CONVERT(varchar, ShortDesc) FROM VW_Sample_Process_Tracking m WHERE m.ReportNo=a.ReportNO  FOR XML PATH('')), 1, 1, '') TestName ,
a.ReportPrintUser
FROM VW_Sample_Process_Tracking a
WHERE a.ReportPrintStatus='Printed' AND a.ReportProcessStatus='Pending' AND Convert(date,a.ReportPrintTime,108)=@date
GROUP BY a.ReportNo,a.InvNo,a.InvDate,a.ReportPrintUser
Order BY a.ReportNo asc
END
GO
/****** Object:  StoredProcedure [dbo].[SP_Pending_ReportReceiveInDeliveryCounter]    Script Date: 3/12/2026 7:00:41 AM ******/
SET ANSI_NULLS ON
GO
SET QUOTED_IDENTIFIER ON
GO

CREATE PROC [dbo].[SP_Pending_ReportReceiveInDeliveryCounter] (@date date,@ptStatus nvarchar(50))AS
BEGIN
SELECT a.InvNo,a.InvDate, a.ReportNo,STUFF((SELECT ',' +  CONVERT(varchar, ShortDesc) FROM VW_Sample_Process_Tracking m WHERE m.ReportNo=a.ReportNO      FOR XML PATH('')), 1, 1, '') TestName ,a.ReportProcessUser,a.PtStatus
FROM VW_Sample_Process_Tracking a
WHERE a.ReportProcessStatus='Processed' AND a.RReceiveInDelCounterStatus='Pending' AND a.PtStatus=@ptStatus AND Convert(date,a.ReceiveInLabTime,108)=@date
GROUP BY a.ReportNo,a.InvNo,a.InvDate,a.ReportProcessUser,a.PtStatus
Order BY a.ReportNo asc
END
GO