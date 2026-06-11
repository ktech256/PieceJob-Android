package com.piecejob.customer.ui.corporate

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.piecejob.core.data.remote.dto.CompanyDto
import com.piecejob.core.data.remote.dto.CorporateScheduleDto
import com.piecejob.core.data.remote.dto.UserDto

@Composable
fun CorporateProfileScreen(
    company: CompanyDto?,
    employees: List<UserDto>,
    schedules: List<CorporateScheduleDto>
) {
    var activeTab by remember { mutableStateOf(0) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.White)
            .padding(16.dp)
    ) {
        Text(
            text = company?.name ?: "Corporate Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF410200)
        )
        Text(text = "Registration: ${company?.registrationNumber ?: "N/A"}", fontSize = 12.sp, color = Color.Gray)

        Spacer(modifier = Modifier.height(24.dp))

        TabRow(selectedTabIndex = activeTab, containerColor = Color.Transparent, contentColor = Color(0xFF410200)) {
            Tab(selected = activeTab == 0, onClick = { activeTab = 0 }, text = { Text("Fleet", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = activeTab == 1, onClick = { activeTab = 1 }, text = { Text("Schedules", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
            Tab(selected = activeTab == 2, onClick = { activeTab = 2 }, text = { Text("Documents", fontSize = 12.sp, fontWeight = FontWeight.Bold) })
        }

        Spacer(modifier = Modifier.height(16.dp))

        when (activeTab) {
            0 -> EmployeeList(employees)
            1 -> ScheduleList(schedules)
            2 -> DocumentList()
        }
    }
}

@Composable
fun EmployeeList(employees: List<UserDto>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(employees) { emp ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(modifier = Modifier.size(40.dp).background(Color(0xFFEEEEEE), RoundedCornerShape(12.dp)), contentAlignment = Alignment.Center) {
                        Text(text = "${emp.firstName[0]}${emp.lastName[0]}", fontWeight = FontWeight.Bold, color = Color.Gray)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(text = "${emp.firstName} ${emp.lastName}", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                        Text(text = emp.role, fontSize = 10.sp, color = Color.Gray)
                    }
                }
            }
        }
    }
}

@Composable
fun ScheduleList(schedules: List<CorporateScheduleDto>) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        items(schedules) { sch ->
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFFF9F9F9))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = sch.serviceCode, fontWeight = FontWeight.Black, fontSize = 14.sp)
                        Text(text = sch.frequency, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color(0xFF410200))
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(text = "Next Run: ${sch.nextRunDate}", fontSize = 12.sp, color = Color.Gray)
                }
            }
        }
    }
}

@Composable
fun DocumentList() {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Document Management Coming Soon", color = Color.LightGray, fontWeight = FontWeight.Bold)
    }
}
